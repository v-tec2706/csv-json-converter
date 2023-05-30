import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import model.{Running, TaskId}
import repository.InMemoryTaskRepository
import service.{TaskExecutor, TaskService}
import common.Mapper.{ResponseFromZIO, requestToTask, requestToTaskId}
import common.NotFoundError
import zio._
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.http.ChannelEvent.UserEventTriggered
import zio.http._
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}

import java.io.File

object App extends ZIOAppDefault {

  private def socket(taskService: TaskService, id: TaskId): Http[Any, Throwable, WebSocketChannelEvent, AnyVal] = Http.collectZIO[WebSocketChannelEvent] {
    case ChannelEvent(channel, UserEventTriggered(HandshakeComplete)) =>
      val task = taskService.get(id).someOrFail(NotFoundError)
      task.flatMap(t => channel.writeAndFlush(WebSocketFrame.text(t.asJson.noSpaces)))
        .repeatUntilZIO(_ => task.fold(_ => false, _.taskState == Running))
        .schedule(Schedule.fixed(2.minutes))
  }

  private def app(taskService: TaskService): App[Any] = {
    Http.collectZIO[Request] {
      case Method.GET -> !! / "tasks" => taskService.getAll.toResponse
      case Method.DELETE -> !! / "task" / id => taskService.cancel(requestToTaskId(id)).toResponse
      case req @ Method.POST -> !! / "task" => requestToTask(req).flatMap(taskService.schedule).toResponse
      case Method.GET -> !! / "task" / id => socket(taskService, requestToTaskId(id)).toSocketApp.toResponse
    } ++ Http.collectHttp[Request] {
      case Method.GET -> !! / "result" / id => Http.fromFile(new File(s"data/result-$id.json"))
    }
  }.catchAllZIO(e => ZIO.succeed(Response.text(e.getMessage).withStatus(Status.BadRequest)))

  private val setup: ZIO[Any, Throwable, TaskService] = for {
    config <- config.ConfigProvider.default.config
    queue <- Queue.unbounded[model.Task]
    tasks <- Ref.make(Map.empty[TaskId, model.Task])
    taskRepository = new InMemoryTaskRepository(tasks)
    _ <- new TaskExecutor(queue, taskRepository, "data").run.provide(ZClient.default).fork
    taskService = new TaskService(queue, taskRepository)
  } yield taskService

  override val run = setup.flatMap(taskService => Server.serve(app(taskService)).provide(Server.default))
}
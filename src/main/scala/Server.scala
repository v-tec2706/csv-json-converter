import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax.EncoderOps
import model.{Task, TaskId}
import repository.InMemoryTaskRepository
import service.{TaskExecutor, TaskService}
import zio._
import zio.http.ChannelEvent.{ChannelRegistered, UserEventTriggered}
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.http._
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}

import java.io.File
import java.util.UUID

object HelloWorld extends ZIOAppDefault {

  def socket(taskService: TaskService, id: TaskId) = Http.collectZIO[WebSocketChannelEvent] {
    case ChannelEvent(ch, UserEventTriggered(HandshakeComplete)) =>
      taskService.get(id).someOrFail(throw new IllegalStateException(""))
        .flatMap(x => ch.writeAndFlush(WebSocketFrame.text(x.asJson.noSpaces))
          .repeatUntil(_ => true).schedule(Schedule.fixed(10.seconds)))
    case other => ZIO.succeed(println(s"Other case ${other}"))
  }

  def app(taskService: TaskService): App[Any] = {
    Http.collectZIO[Request] {
      case Method.GET -> !! / "tasks" => taskService.getAll.map(ok => Response.json(ok.asJson.noSpaces))
      case Method.DELETE -> !! / "task" / id => taskService.cancel(TaskId(UUID.fromString(id))).map(ok => Response.json(ok.asJson.noSpaces))
      case req@Method.POST -> !! / "task" =>
        req.body.asString.map(s => parser.parse(s).flatMap(_.hcursor.downField("filePath").as[String]).toOption).someOrFail(new IllegalStateException(""))
          .flatMap(s =>
            taskService.schedule(Task.fromFilePath(s)))
          .map(ok => Response.json(ok.asJson.noSpaces))
      case Method.GET -> !! / "task" / id => socket(taskService, TaskId(UUID.fromString(id))).toSocketApp.toResponse
    } ++ Http.collectHttp[Request] {
      case Method.GET -> !! / "result" / id => Http.fromFile(new File(s"data/result-$id.json"))
    }
  }.catchAllZIO(e => ZIO.succeed(Response.text(e.getMessage).withStatus(Status.BadRequest)))

  val setup: ZIO[Any, Nothing, TaskService] = for {
    queue <- Queue.unbounded[model.Task]
    tasks <- Ref.make(Map.empty[TaskId, model.Task])
    taskRepository = new InMemoryTaskRepository(tasks)
    _ <- new TaskExecutor(queue, taskRepository, "data").run.provide(ZClient.default).fork
    taskService = new TaskService(queue, taskRepository)
  } yield taskService

  override val run = setup.flatMap(taskService => Server.serve(app(taskService)).provide(Server.default))

}
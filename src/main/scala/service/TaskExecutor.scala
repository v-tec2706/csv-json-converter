package service

import io.circe.syntax.EncoderOps
import model._
import repository.TaskRepository
import zio.http.{Body, Client}
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.{Queue, ZIO}

class TaskExecutor(activeTasks: Queue[Task], taskRepository: TaskRepository, outputPath: String) {
  private val delimiter = ","

  def run: ZIO[Client, Throwable, Unit] = (for {
    elems <- ZStream.fromZIO(activeTasks.takeAll)
    _ <- ZStream.fromChunk(elems).mapZIOPar(2)(taskProcessing)
  } yield ()).forever.run(ZSink.drain)

  private def taskProcessing(task: Task): ZIO[Client, Throwable, Product] = {
    (for {
      _ <- taskRepository.update(task.copy(taskState = Running))
      task <- taskRepository.getOrFail(task.taskId)
      data <- Client.request(task.sourceUrl.url)
      result <- processFile(task, data.body)
      _ <- taskRepository.update(result.copy(taskState = Done))
    } yield ())
      .whenZIO(taskRepository.getOrFail(task.taskId).map(_.taskState == Scheduled))
      .tapError(e => ZIO.succeed(println(e.getMessage)))
      .orElse(taskRepository.update(task.copy(taskState = Failed)))
  }

  private def processFile(task: Task, body: Body): ZIO[Any, Throwable, Task] = {
    val input = body.asStream.via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
    val path = s"$outputPath/result-${task.taskId.id}.json"
    val fileSink = ZSink.fromFileName(path).contramapChunks[String](_.flatMap(_.getBytes))

    input.mapAccum(Option.empty[List[String]]) {
      case (header, line) => header match {
        case Some(headerItems) => (header, Some(headerItems.zip(line.split(delimiter).toList)))
        case None => (Some(line.split(delimiter).toList), None)
      }
    }.collectSome.map(_.toMap)
      .map(_.asJson.spaces2)
      .intersperse("[", ",", "]")
      .run(fileSink)
      .as(task.copy(resultPath = Some(new Path(path))))
  }
}

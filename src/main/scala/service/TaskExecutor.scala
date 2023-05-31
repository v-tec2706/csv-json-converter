package service

import config.Config
import io.circe.syntax.EncoderOps
import model._
import repository.{ResultStorage, TaskRepository}
import zio.http.{Body, Client}
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.{Queue, ZIO}

class TaskExecutor(activeTasks: Queue[Task], taskRepository: TaskRepository, resultStorage: ResultStorage, config: Config) {
  private val delimiter = config.csvDelimiter

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
      .orElse(taskRepository.update(task.copy(taskState = Failed)))
  }

  private def processFile(task: Task, body: Body): ZIO[Any, Throwable, Task] = {
    val input = body.asStream.via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)

    input.mapAccum(Option.empty[List[String]]) {
      case (header, line) => header match {
        case Some(headerItems) => (header, Some(headerItems.zip(line.split(delimiter).toList)))
        case None => (Some(line.split(delimiter).toList), None)
      }
    }.collectSome.map(_.toMap)
      .map(_.asJson.spaces2)
      .intersperse("[", ",", "]")
      .run(resultStorage.saveFile(task.taskId))
      .as(task)
  }
}

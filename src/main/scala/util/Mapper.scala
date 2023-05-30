package util

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, parser}
import model.{InvalidInput, Task, TaskId}
import zio.ZIO
import zio.http.{Request, Response}

import java.util.UUID

object Mapper {
  def requestToTask(request: Request): ZIO[Any, Throwable, Task] =
    request.body.asString.map(s => parser.parse(s).flatMap(_.hcursor.downField("filePath").as[String]).toOption)
      .someOrFail(InvalidInput)
      .map(Task.fromFilePath)

  def requestToTaskId(request: String): TaskId = new TaskId(UUID.fromString(request))

  implicit class ResponseFromZIO[A: Encoder](responseZIO: ZIO[Any, Throwable, A]) {
    def toResponse: ZIO[Any, Throwable, Response] = responseZIO.map(result => Response.json(result.asJson.noSpaces))
  }
}

package common

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, parser}
import model.{Task, TaskId}
import zio.ZIO
import zio.http.{Request, Response, Status}

import java.util.UUID

object Mapper {
  def requestToTask(request: Request): ZIO[Any, Throwable, Task] =
    request.body.asString.map(s => parser.parse(s).flatMap(_.hcursor.downField("filePath").as[String]).toOption)
      .someOrFail(InvalidInput)
      .map(Task.fromFilePath)

  def requestToTaskId(request: String): TaskId = new TaskId(UUID.fromString(request))

  def errorToResponse(error: Throwable): ZIO[Any, Response, Response] =
    ZIO.succeed {
      error match {
        case error: NotFoundError.type => Response.text(error.message).withStatus(Status.NotFound)
        case error: InvalidInput.type => Response.text(error.message).withStatus(Status.BadRequest)
        case _ => Response.status(Status.InternalServerError)
      }
    }

  implicit class ResponseFromZIO[A: Encoder](responseZIO: ZIO[Any, Throwable, A]) {
    def toResponse: ZIO[Any, Throwable, Response] = responseZIO.map(result => Response.json(result.asJson.noSpaces))
  }
}

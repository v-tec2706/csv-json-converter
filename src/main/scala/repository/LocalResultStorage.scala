package repository

import config.Config
import model.{Path, TaskId}
import zio.stream.ZSink

import java.io.File

class LocalResultStorage(config: Config) extends ResultStorage {
  private val filePrefix = "result"

  override def storageFile(taskId: TaskId): ZSink[Any, Throwable, String, Byte, Long] = {
    val path = s"${config.fileStoragePath}/$filePrefix-${taskId.id}.json"
    ZSink.fromFileName(path).contramapChunks[String](_.flatMap(_.getBytes))
  }

  override def getTaskResult(taskId: TaskId): File = new File(s"${config.fileStoragePath}/$filePrefix-${taskId.id}.json")
}

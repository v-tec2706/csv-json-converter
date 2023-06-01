package repository

import model.TaskId
import zio.stream.ZSink

import java.io.File

trait ResultStorage {
  def saveFile(taskId: TaskId): ZSink[Any, Throwable, String, Byte, Long]
  def getTaskResult(taskId: TaskId): File
}

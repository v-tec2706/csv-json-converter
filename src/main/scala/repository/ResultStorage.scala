package repository

import model.{Path, TaskId}
import zio.stream.ZSink

import java.io.File

trait ResultStorage {
  def storageFile(taskId: TaskId): ZSink[Any, Throwable, String, Byte, Long]
  def getTaskResult(taskId: TaskId): File
}

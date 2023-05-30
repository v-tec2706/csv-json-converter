package model

import java.util.UUID

case class TaskId(id: UUID) extends AnyVal
case class Url(url: String) extends AnyVal
case class Path(path: String) extends AnyVal

sealed trait TaskState
case object Scheduled extends TaskState
case object Running extends TaskState
case object Done extends TaskState
case object Failed extends TaskState
case object Cancelled extends TaskState

case class Task(taskId: TaskId, sourceUrl: Url, taskState: TaskState, resultPath: Option[Path])
object Task {
  def fromFilePath(filePath: String): Task = Task(TaskId(UUID.randomUUID()), Url(filePath), Scheduled, None)
}


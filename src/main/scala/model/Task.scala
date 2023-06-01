package model

import java.util.UUID

class TaskId(val id: UUID) extends AnyVal
class Url(val url: String) extends AnyVal
class Path(val path: String) extends AnyVal

sealed trait TaskState
case object Scheduled extends TaskState
case object Running extends TaskState
case object Done extends TaskState
case object Failed extends TaskState
case object Cancelled extends TaskState

case class Task(taskId: TaskId, sourceUrl: Url, taskState: TaskState)
object Task {
  def fromFilePath(filePath: String): Task = Task(new TaskId(UUID.randomUUID()), new Url(filePath), Scheduled)
}


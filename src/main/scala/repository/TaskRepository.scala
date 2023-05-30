package repository

import model.{NotFoundError, Task, TaskId}
import zio.ZIO

trait TaskRepository {

  def add(task: Task): ZIO[Any, Throwable, Task]
  def update(task: Task): ZIO[Any, Throwable, Task]
  def get(taskId: TaskId): ZIO[Any, Throwable, Option[Task]]
  def getOrFail(taskId: TaskId): ZIO[Any, Throwable, Task] = get(taskId).someOrFail(NotFoundError)
  def getAll: ZIO[Any, Throwable, List[Task]]
}

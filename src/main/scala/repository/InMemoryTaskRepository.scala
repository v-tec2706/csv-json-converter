package repository

import model.{Task, TaskId}
import zio.{Ref, ZIO}

class InMemoryTaskRepository(tasks: Ref[Map[TaskId, Task]]) extends TaskRepository {
  override def add(task: Task): ZIO[Any, Nothing, Task] = tasks.update(t => t + (task.taskId -> task)).as(task)
  override def update(task: Task): ZIO[Any, Throwable, Task] = tasks.update(_.updated(task.taskId, task)).as(task)
  override def get(taskId: TaskId): ZIO[Any, Nothing, Option[Task]] = tasks.get.map(_.get(taskId))
  override def getAll: ZIO[Any, Throwable, List[Task]] = tasks.get.map(_.values).map(_.toList)
}

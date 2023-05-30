package service

import model.{Cancelled, Task, TaskId}
import repository.TaskRepository
import zio.{Queue, ZIO}

class TaskService(queue: Queue[Task], tasksRepository: TaskRepository) {

  def get(taskId: TaskId): ZIO[Any, Throwable, Option[Task]] = tasksRepository.get(taskId)
  def getAll: ZIO[Any, Throwable, List[Task]] = tasksRepository.getAll
  def schedule(task: Task): ZIO[Any, Throwable, Boolean] = tasksRepository.add(task) *> queue.offer(task)
  def cancel(taskId: TaskId): ZIO[Any, Throwable, Task] =
    tasksRepository.getOrFail(taskId).flatMap(task => tasksRepository.update(task.copy(taskState = Cancelled)))
}

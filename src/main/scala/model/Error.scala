package model

sealed trait Error extends Throwable {
  def message: String
}

case object InvalidInput extends Error {
  override def message: String = "Cannot parse input argument"
}

case object NotFoundError extends Error {
  override def message: String = "Cannot find item with given ID"
}


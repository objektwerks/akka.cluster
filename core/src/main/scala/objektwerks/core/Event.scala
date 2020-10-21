package objektwerks.core

sealed trait Event extends Product with Serializable

final case class FactorialDone(id: Id, output: Factorial) extends Event

case object WorkTimedOut extends Event

final case class WorkFailed(id: Id) extends Event
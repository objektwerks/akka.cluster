package objektwerks.core

sealed trait Event extends Product with Serializable

final case class FactorialComputed(id: Id, factorialOut: Factorial) extends Event

case object FactorialTimedOut extends Event

final case class FactorialFailed(id: Id) extends Event
package objektwerks.core

sealed trait Command

case object GetFactorial extends Command

final case class DoFactorial(id: Id, input: Factorial) extends Command
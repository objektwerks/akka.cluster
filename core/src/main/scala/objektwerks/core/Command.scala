package objektwerks.core

sealed trait Command extends Product with Serializable

case object PullFactorial extends Command

final case class ComputeFactorial(id: Id, factorialIn: Factorial) extends Command
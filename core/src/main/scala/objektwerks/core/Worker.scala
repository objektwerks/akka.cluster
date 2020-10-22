package objektwerks.core

import akka.actor.{Actor, ActorLogging}

import scala.annotation.tailrec

class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case ComputeFactorial(id, factorialIn) =>
      log.info("*** factorial in: {} : {}", id.queueId, factorialIn)
      val factorialOut = Factorial(factorialIn.numberIn, factorial(factorialIn.numberIn))
      log.info("*** factorial out: {} : {}", id.queueId, factorialOut)
      sender() ! FactorialComputed(id, factorialOut)
  }

  @tailrec
  private def factorial(number: Long, accumulator: Long = 1): Long = number match {
    case i if i < 1 => accumulator
    case _ => factorial(number - 1, accumulator * number)
  }
}
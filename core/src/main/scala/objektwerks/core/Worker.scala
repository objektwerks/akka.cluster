package objektwerks.core

import akka.actor.{Actor, ActorLogging}

import scala.annotation.tailrec

class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case DoFactorial(id, factorialIn) =>
      log.info("*** factorial in: {} : {}", id.queueId, factorialIn)
      val factorialOut = Factorial(factorialIn.input, factorial(factorialIn.input))
      log.info("*** factorial out: {} : {}", id.queueId, factorialOut)
      sender() ! FactorialDone(id, factorialOut)
  }

  @tailrec
  private def factorial(n: Long, acc: Long = 1): Long = n match {
    case i if i < 1 => acc
    case _ => factorial(n - 1, acc * n)
  }
}
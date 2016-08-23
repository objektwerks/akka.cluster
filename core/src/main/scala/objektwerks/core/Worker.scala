package objektwerks.core

import akka.actor.{Actor, ActorLogging}

import scala.annotation.tailrec

class Worker extends Actor with ActorLogging {
  override def receive: Receive = {
    case command @ DoFactorial(id, input) =>
      log.info("*** input: {} : {}", id.queueId, input)
      val output = Factorial(input.input, factorial(input.input))
      log.info("*** answer: {} : {}", id.queueId, output)
      sender ! FactorialDone(id, output)
  }

  @tailrec
  private def factorial(n: Long, acc: Long = 1): Long = n match {
    case i if i < 1 => acc
    case _ => factorial(n - 1, acc * n)
  }
}
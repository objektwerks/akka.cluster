package objektwerks.core

import akka.actor.{Actor, ActorRef, ReceiveTimeout}

import scala.concurrent.duration._
import scala.language.postfixOps

class Master(broker: ActorRef, workerRouter: ActorRef) extends Actor {
  context.setReceiveTimeout(3 minutes)

  override def receive: Receive = {
    case command @ ComputeFactorial(_, _) =>
      implicit val ec = context.dispatcher
      context.system.scheduler.scheduleOnce(100 millis, workerRouter, command)
      ()
    case event @ FactorialComputed(_, _) =>
      broker ! event
      context.stop(self)
    case ReceiveTimeout =>
      broker ! FactorialTimedOut
      context.stop(self)
  }
}
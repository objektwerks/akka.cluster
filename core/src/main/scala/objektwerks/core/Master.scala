package objektwerks.core

import akka.actor.{Actor, ActorRef, ReceiveTimeout}

import scala.concurrent.duration._
import scala.language.postfixOps

class Master(broker: ActorRef, workerRouter: ActorRef) extends Actor {
  context.setReceiveTimeout(3 minutes)

  override def receive: Receive = {
    case command @ DoFactorial(_, _) =>
      implicit val ec = context.dispatcher
      context.system.scheduler.scheduleOnce(100 millis, workerRouter, command)
      ()
    case event @ FactorialDone(_, _) =>
      broker ! event
      context.stop(self)
    case ReceiveTimeout =>
      broker ! WorkTimedOut
      context.stop(self)
  }
}
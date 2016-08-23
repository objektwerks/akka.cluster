package objektwerks.core

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}

import scala.concurrent.duration._

object Master {
  def props(broker: ActorRef, workerRouter: ActorRef): Props = Props(classOf[Master], broker, workerRouter)
}

class Master(broker: ActorRef, workerRouter: ActorRef) extends Actor {
  context.setReceiveTimeout(60 minutes)

  override def receive: Receive = {
    case command @ DoFactorial(id, input) =>
      implicit val ec = context.dispatcher
      context.system.scheduler.scheduleOnce(100 millis, workerRouter, command)
    case event @ FactorialDone(id, output) =>
      broker ! event
      context.stop(self)
    case ReceiveTimeout =>
      broker ! WorkTimedOut
      context.stop(self)
  }
}
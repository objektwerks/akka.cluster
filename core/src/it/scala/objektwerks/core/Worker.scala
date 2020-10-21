package objektwerks.core

import akka.actor.Actor

class Worker extends Actor {
  override def receive: Receive = {
    case Request(id, message) => sender() ! Response(id, message)
  }
}
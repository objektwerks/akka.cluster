package objektwerks.core

import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorLogging, Props}

object Queue {
  def props(requestQueue: QueueConnector, responseQueue: QueueConnector): Props = Props(classOf[Queue], requestQueue, responseQueue)
}

class Queue(requestQueue: QueueConnector, responseQueue: QueueConnector) extends Actor with ActorLogging {
  override def receive: Receive = {
    case PullRequest =>
      requestQueue.pull match {
        case Some(item) =>
          val id = item.getEnvelope.getDeliveryTag
          val message = new String(item.getBody, StandardCharsets.UTF_8)
          log.debug("request: {}", id)
          sender ! Request(id, message)
        case None =>
          sender ! Shutdown
      }
    case Response(id, message) =>
      log.debug("response: {}", id)
      val wasPushed = responseQueue.push(message)
      require(wasPushed)
      requestQueue.ack(id)
  }
}
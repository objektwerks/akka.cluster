package objektwerks.core

import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorLogging}

import com.typesafe.config.ConfigFactory

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

class Queue extends Actor with ActorLogging {
  val requestQueue = new QueueConnector(ConfigFactory.load("request.queue.conf").as[QueueConnectorConf]("queue"))
  val responseQueue = new QueueConnector(ConfigFactory.load("response.queue.conf").as[QueueConnectorConf]("queue"))

  override def receive: Receive = {
    case GetFactorial =>
      requestQueue.pull.foreach { item =>
          val queueId = item.getEnvelope.getDeliveryTag
          val id = Id(queueId)
          val json = new String(item.getBody, StandardCharsets.UTF_8)
          log.info("*** request queue id: {} : {}", queueId, json)
          val input = Factorial.toFactorial(json)
          sender() ! DoFactorial(id, input)
      }
    case FactorialDone(id, output) =>
      val json = Factorial.toJson(output)
      val isConfirmed = responseQueue.push(json)
      if (isConfirmed) requestQueue.ack(id.queueId) else self ! WorkFailed(id)
      log.info("*** response queue id: {} in {} : {}", id.queueId, id.duration, json)
    case WorkFailed(id) =>
      log.info("*** request queue failure id: {}", id.queueId)
      requestQueue.nack(id.queueId)
  }
}
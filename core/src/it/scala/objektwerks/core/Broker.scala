package objektwerks.core

import akka.actor.{Actor, ActorLogging, Props}

import com.typesafe.config.ConfigFactory

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

class Broker extends Actor with ActorLogging {
  val requestQueue = new QueueConnector(ConfigFactory.load("test.request.queue.conf").as[QueueConnectorConf]("queue"))
  val responseQueue = new QueueConnector(ConfigFactory.load("test.response.queue.conf").as[QueueConnectorConf]("queue"))
  val queue = context.actorOf(Queue.props(requestQueue, responseQueue), name = "queue")
  val worker = context.actorOf(Props[Worker](), name = "worker")

  override def receive: Receive = {
    case PullRequest => queue ! PullRequest
    case request: Request => worker ! request
    case response: Response =>
      queue ! response
      queue ! PullRequest
    case Shutdown =>
      log.debug("*** broker shutting down...")
      context stop queue
      context stop worker
      context stop self
  }
}
package objektwerks.core

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.util.Timeout

import com.typesafe.config.ConfigFactory

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class QueueConnectorTest extends AnyFunSuite with BeforeAndAfterAll {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val timeout = Timeout(1 second)

  val system = ActorSystem.create("queue", ConfigFactory.load("test.akka.conf"))
  val broker = system.actorOf(Props[Broker](), name = "broker")

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 3 seconds)
    ()
  }

  test("push pull") {
    val queue = new QueueConnector(ConfigFactory.load("test.queue.conf").as[QueueConnectorConf]("queue"))
    clearQueue(queue)
    logger.debug("*** push pull test: test rabbitmq queue cleared!")
    pushMessagesToRequestQueue(queue, 10)
    pullMessagesFromRequestQueue(queue, 10)
    queue.close()
  }

  test("broker") {
    val requestQueue = new QueueConnector(ConfigFactory.load("test.request.queue.conf").as[QueueConnectorConf]("queue"))
    clearQueue(requestQueue)
    logger.debug("*** broker test: request queue cleared!")
    pushMessagesToRequestQueue(requestQueue, 10)
    requestQueue.close()
    broker ! PullRequest
    Thread.sleep(1000)
    val responseQueue = new QueueConnector(ConfigFactory.load("test.response.queue.conf").as[QueueConnectorConf]("queue"))
    clearQueue(responseQueue)
    logger.debug("*** broker test: response queue cleared!")
    responseQueue.close()
  }

  private def pushMessagesToRequestQueue(queue: QueueConnector, count: Int): Unit = {
    val counter = new AtomicInteger()
    val confirmed = new AtomicInteger()
    for (_ <- 1 to count) {
      val message = s"*** test.request: ${counter.incrementAndGet}"
      val isComfirmed = queue.push(message)
      if (isComfirmed) confirmed.incrementAndGet
    }
    assert(confirmed.intValue == count)
    ()
  }

  private def pullMessagesFromRequestQueue(queue: QueueConnector, count: Int): Unit = {
    val pulled = new AtomicInteger()
    for (_ <- 1 to count) if(queue.pull.nonEmpty) pulled.incrementAndGet
    assert(pulled.intValue == count)
    ()
  }

  private def clearQueue(queue: QueueConnector): Unit = {
    var queueIsEmpty = false
    while (!queueIsEmpty) {
      queueIsEmpty = queue.pull.isEmpty
    }
  }
}
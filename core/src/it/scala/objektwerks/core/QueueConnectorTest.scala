package objektwerks.core

import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.LoggerFactory

class QueueConnectorTest extends AnyFunSuite with BeforeAndAfterAll {
  val logger = LoggerFactory.getLogger(getClass)

  override protected def afterAll(): Unit = {
    val queue = new QueueConnector(ConfigFactory.load("request.queue.conf").as[QueueConnectorConf]("queue"))
    for (i <- 1 to 10) {
      val factorial = Factorial(numberIn = i, numberOut = 0)
      queue.push( Factorial.toJson(factorial) )
    }
    queue.close()
    logger.info("*** 10 Factorial json messages pushed to request.queue")
  }

  test("push > pull") {
    val queue = new QueueConnector(ConfigFactory.load("test.queue.conf").as[QueueConnectorConf]("queue"))
    pushMessagesToRequestQueue(queue, 10)
    pullMessagesFromRequestQueue(queue, 10)
    queue.close()
  }

  private def pushMessagesToRequestQueue(queue: QueueConnector, count: Int): Unit = {
    val counter = new AtomicInteger()
    val confirmed = new AtomicInteger()
    for (_ <- 1 to count) {
      val message = s"message[${counter.incrementAndGet}]"
      val isComfirmed = queue.push(message)
      if (isComfirmed) confirmed.incrementAndGet
    }
    assert(confirmed.intValue == count)
    ()
  }

  private def pullMessagesFromRequestQueue(queue: QueueConnector, count: Int): Unit = {
    val pulled = new AtomicInteger()
    for (_ <- 1 to count) {
      if (queue.pull.nonEmpty) pulled.incrementAndGet
    }
    assert(pulled.intValue == count)
    ()
  }
}
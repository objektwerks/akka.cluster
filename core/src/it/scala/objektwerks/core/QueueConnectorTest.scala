package objektwerks.core

import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.ConfigFactory

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.scalatest.funsuite.AnyFunSuite

class QueueConnectorTest extends AnyFunSuite {
  test("push > pull") {
    val queue = new QueueConnector(ConfigFactory.load("test.queue.conf").as[QueueConnectorConf]("queue"))
    clearQueue(queue)
    pushMessagesToRequestQueue(queue, 10)
    pullMessagesFromRequestQueue(queue, 10)
    queue.close()
  }

  private def clearQueue(queue: QueueConnector): Unit = {
    var queueIsEmpty = false
    while (!queueIsEmpty) {
      queueIsEmpty = queue.pull.isEmpty
    }
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
}
package objektwerks.core

import java.lang.management.ManagementFactory
import java.time.{Duration, LocalDateTime}
import javax.management.ObjectName

import play.api.libs.json.Json

final case class Id(queueId: Long, received: LocalDateTime = LocalDateTime.now) {
  def duration: String = Durations.average(Duration.between(received, LocalDateTime.now))
}

final case class Factorial(input: Long, output: Long)

object Factorial {
  private implicit val factorialFormat = Json.format[Factorial]
  private implicit val factorialFormatRead = Json.reads[Factorial]
  private implicit val factorialFormatWrite = Json.writes[Factorial]

  def toJson(math: Factorial): String = Json.toJson(math).toString
  def toFactorial(json: String): Factorial = Json.parse(json).as[Factorial]
}

private object Durations {
  import scala.collection.mutable
  private val durations = new mutable.ArrayBuffer[Long]()
  private val mbean = new DurationsTracker()
  private val mbeanName = new ObjectName("objektwerks.masternode:type=durations")
  ManagementFactory.getPlatformMBeanServer.registerMBean(mbean, mbeanName)

  def average(duration: Duration): String = {
    durations.synchronized {
      durations += duration.toMillis
      val numberOfDurations = durations.size
      val averageOfDurations = durations.sum / numberOfDurations
      val formattedDuration = format(duration)
      val formattedAverage = format(Duration.ofMillis(averageOfDurations))
      val value = "%s, averaging %s over %d request/response pairs".format(formattedDuration, formattedAverage, numberOfDurations)
      mbean.setDurations(value)
      value
    }
  }

  private def format(duration: Duration): String = {
    if (duration.toMillis < 1000)
      "%d millis".format(duration.toMillis)
    else if (duration.toMinutes == 0)
      "%d second(s)".format(duration.toMillis / 1000)
    else
      "%d minute(s)".format(duration.toMinutes)
  }
}

trait DurationsMXBean {
  def getDurations: String
  def setDurations(durations: String): Unit
}

class DurationsTracker extends DurationsMXBean {
  private var durations = ""
  override def getDurations: String = durations
  override def setDurations(durations: String): Unit = this.durations = durations
}
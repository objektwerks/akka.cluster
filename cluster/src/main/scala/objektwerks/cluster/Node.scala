package objektwerks.cluster

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class Node extends App {
  if (args.isEmpty) throw new IllegalArgumentException("arg 0 must specify the node conf file.")

  private implicit val timeout = Timeout(30 seconds)
  private val conf = args(0)
  private val config = ConfigFactory.load(conf)
  private val appName = config.getString("app")
  private val clusterEventListenerEnabled = config.getBoolean("clusterEventListenerEnabled")
  private val clusterMetricsListenerEnabled = config.getBoolean("clusterMetricsListenerEnabled")

  protected val system = ActorSystem.create(appName, config)
  if (clusterEventListenerEnabled) system.actorOf(Props[ClusterEventListener])
  if (clusterMetricsListenerEnabled) system.actorOf(Props[ClusterMetricsListener])

  sys.addShutdownHook {
    implicit val ec = system.dispatcher
    Await.result(system.terminate, 30 seconds)
  }
}
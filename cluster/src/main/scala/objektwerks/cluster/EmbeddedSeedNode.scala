package objektwerks.cluster

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class EmbeddedSeedNode(app: String,
                       conf: String,
                       host: String,
                       port: Int,
                       seeds: (String, String)) {
  implicit private val timeout = Timeout(10 seconds)
  private val config = ConfigFactory.parseString(s"""akka.remote.netty.tcp {
                                                   |   hostname = $host
                                                   |   port = $port
                                                   |}
                                                   |akka.cluster.seed-nodes = [
                                                      akka.tcp://$app@$host:$seeds._1,
                                                      akka.tcp://$app@$host:$seeds._2
                                                    ]""").withFallback(ConfigFactory.load(conf))
  private val system = ActorSystem.create(app, config)

  def terminate(): Unit = {
    implicit val ec = system.dispatcher
    Await.result(system.terminate(), 3 seconds)
  }
}
package objektwerks.cluster

import akka.actor.Props
import objektwerks.core.Worker

object WorkerNode extends Node {
  system.actorOf(Props[Worker](), name = "worker")
}
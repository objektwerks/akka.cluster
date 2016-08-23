package objektwerks.cluster

import akka.actor.Props
import akka.cluster.Cluster
import objektwerks.core.Broker

object MasterNode extends Node {
  Cluster(system).registerOnMemberUp {
    system.actorOf(Props[Broker], name = "broker")
  }
}
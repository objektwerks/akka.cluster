package objektwerks.cluster

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ClusterDomainEvent, InitialStateAsEvents}

class ClusterEventListener extends Actor with ActorLogging {
  override def preStart(): Unit = {
    Cluster(context.system).subscribe(self, initialStateMode = InitialStateAsEvents, classOf[ClusterDomainEvent])
  }

  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
  }

  override def receive: Receive = {
    case event: ClusterDomainEvent => log.info("Actor: {} Event: {}", sender.path.name, event)
  }
}
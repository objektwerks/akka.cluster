package objektwerks.cluster

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.metrics.StandardMetrics.{Cpu, HeapMemory}
import akka.cluster.metrics.{ClusterMetricsChanged, ClusterMetricsExtension, NodeMetrics}

class ClusterMetricsListener extends Actor with ActorLogging {
  private val system = context.system
  private val cluster = Cluster(system)
  private val metrics = ClusterMetricsExtension.get(system)

  override def preStart(): Unit = {
    metrics.subscribe(self)
  }

  override def postStop(): Unit = {
    metrics.unsubscribe(self)
  }

  override def receive: Receive = {
    case ClusterMetricsChanged(clusterMetrics) =>
      clusterMetrics.filter(_.address == cluster.selfAddress) foreach { nodeMetrics =>
        logHeap(nodeMetrics)
        logCpu(nodeMetrics)
      }
  }

  private def logHeap(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case HeapMemory(_, _, used, _, _) =>
      log.info("Used heap: %.2f MB".format(used.doubleValue / 1024 / 1024))
  }

  private def logCpu(nodeMetrics: NodeMetrics): Unit = nodeMetrics match {
    case Cpu(_, _, Some(systemLoadAverage), _, _, processors) =>
      log.info("Average core load: %.2f on %d cores".format(systemLoadAverage, processors))
  }
}
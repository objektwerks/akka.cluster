package objektwerks.core

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberRemoved, MemberUp, ReachableMember}
import akka.cluster.routing.{ClusterRouterGroup, ClusterRouterGroupSettings}
import akka.routing.RoundRobinGroup

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.language.postfixOps

sealed trait WorkerRouter {
  this: Actor =>
  val group = RoundRobinGroup(List("/user/worker"))
  val settings = ClusterRouterGroupSettings(
    totalInstances = 100,
    routeesPaths = List("/user/worker"),
    allowLocalRoutees = false,
    useRoles = "worker")
  val workerRouter = context.actorOf(ClusterRouterGroup(group, settings).props(), name = "workerRouter")
}

class Broker extends Actor with WorkerRouter with ActorLogging {
  implicit val ec = context.dispatcher
  val availableWorkers = new AtomicInteger()
  val masterNumber = new AtomicInteger()
  val masterToIdMapping = TrieMap.empty[ActorRef, Id]
  val queue = context.actorOf(Props[Queue](), name = "queue")
  val newMasterName = () => s"master-${masterNumber.incrementAndGet()}"

  def runFactorial: Runnable = 
    new Runnable {
      override def run(): Unit = if(availableWorkers.intValue > masterToIdMapping.size) queue ! GetFactorial
    }

  context.system.scheduler.scheduleWithFixedDelay(1 minute, 10 seconds)(runFactorial)

  override def receive: Receive = {
    case command: DoFactorial =>
      val master = context.actorOf(Props(classOf[Master], self, workerRouter), name = newMasterName())
      masterToIdMapping += (master -> command.id)
      master ! command
    case event: FactorialDone =>
      masterToIdMapping -= sender()
      queue ! event
      queue ! GetFactorial
    case WorkTimedOut =>
      val id = masterToIdMapping.remove(sender()).getOrElse(Id(-1))
      queue ! WorkFailed(id)
    case MemberUp(member) =>
      if (member.hasRole("worker")) {
        availableWorkers.incrementAndGet()
        queue ! GetFactorial
        log.info("*** worker up: {}", member.address)
      }
    case MemberRemoved(member, _) =>
      if (member.hasRole("worker")) {
        availableWorkers.decrementAndGet()
        log.info("*** worker removed: {}", member.address)
      }
  }

  override def preStart(): Unit = {
    Cluster(context.system).subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp], classOf[ReachableMember])
  }

  override def postStop(): Unit = Cluster(context.system).unsubscribe(self)
}
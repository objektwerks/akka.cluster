Framework
---------
>Akka cluster project --- using per-request dynamic master-worker teams. See **viewme.pdf** for details.

Project
-------
>A **multi-project** sbt project:

1. cluster
2. core
3. seednode
4. masternode
5. workernode

Cluster
-------
* seed nodes - 2
* master nodes - 1+
* worker nodes - 2+

Node-Actors
-----------
1. master node - Broker, Queue, Master
2. worker node - Worker

Actors
------
1. Broker - manages a pool of Queue and Master actors
2. Queue - pulls and pushes messages to a RabbitMQ cluster
3. Master - delegates a request to a remote Worker actor
4. Worker - processes a request and returns a response to its Master actor

Routers
-------
1. Worker Router - A cluster-aware group router containing a dynamic pool of available remote Worker actors. See Broker actor.

Design
------
>The cluster is architecturally centered around the Broker actor, which resides on the master node and **dynamically**
pulls messages from RabbitMQ via a Queue actor and creates Master actors just-in-time that delegate work to a
Worker actor on a worker node.


Scheduler
---------
>The Broker actor pulls messages via the Queue actor as follows:

1. On MemberUp cluster events
2. On MapWorkDone message
3. Via this scheduler: ```context.system.scheduler.schedule(1 minute, 10 seconds)(getMapWork())```

>If the scheduler discovers (1) ZERO active Master actors **OR** (2) more available Worker actors than active Master
actors, then Broker actor sends the Queue actor a GetMapWork message.

Tuning
------
>The Master actor, created dynamically by the Broker actor, has a time-to-live , which is currently set to:

    ```context.setReceiveTimeout(3 minutes)```

>When the **time-to-live** is exceeded, a Master actor receives a ReceiveTimout message, self-terminates and notifies
the Broker actor, which tells a Queue actor to send a NACK message to RabbitMQ - retaining the origial request on
RabbitMQ for future processing. This value is **TUNABLE**!

Memory
------
>In the build.sbt, tune the JVM options as required.

```
lazy val workerNodeSettings = commonSettings ++ publishSettings ++ packAutoSettings ++ Seq(
  libraryDependencies ++=  mapContextDependencies,
  libraryDependencies ++=  akkaDependencies,
  dependencyOverrides ++= akkaDependencyOverrides,
  packCopyDependenciesUseSymbolicLinks := false,
  packExcludeJars := Seq("hammer-commons-2.15.jar"),
  packJvmOpts := Map("worker-node" -> Seq("-server", "-Xss1m", "-Xms1g", "-Xmx32g"))
)
```

RabbitMQ
--------
1. Install RabbitMQ ( https://www.rabbitmq.com/download.html )
2. Install RabbitMQ Management Web UI ( https://www.rabbitmq.com/management.html )
3. rabbitmqadmin - https://www.rabbitmq.com/management-cli.html
4. rabbitmqctl - https://www.rabbitmq.com/man/rabbitmqctl.1.man.html
6. Restart:
    1. rabbitmqctl stop_app
    2. rabbitmqctl reset
    3. rabbitmqctl start_app
    4. rabbitmqctl list_queues name messages_ready messages_unacknowledged

>**Note**: A local or network instance of RabbitMQ must be available for (1) integration
tests and (2) deployment ( See: Run > Master Node 1 below for details )

Gerrit
------
1. git clone ssh://mfunk@gerrit.it.here.com:29418/CME/EVA/Framework
2. git add .
3. git commit -m "message"
    1. git add.
    2. git commit —-amend --no-edit
4. git push origin HEAD:refs/for/master

Compile
-------
1. sbt clean compile

Test
----
1. sbt clean test

Integration Test
----------------
1. sbt clean it:test

>Currently the connector integration test pushes **100** Factorial json messages to the request queue,
allowing for just-in-time EVA cluster testing. The Queue actor will automatically pull these messages
from the request queue. If you want to push more messages to the request queue, while the EVA cluster
is up, simply rerun the integration test.

2. ./test.map.geometry.publish.sh

>Currently, this pushes 1 MapGeometry json message to the request queue.

Pack
----
1. sbt clean test it:test pack

>**See:** https://github.com/xerial/sbt-pack

Run
---
>Notes:

1. After running pack, run these scripts and nodes in order.
2. Additional worker nodes, running on different ports, can be run.

> Scripts:

1. chmod u+x ./run.seed.node.sh
1. chmod u+x ./run.worker.node.sh
1. chmod u+x ./run.master.node.sh

> Nodes:

1. Seed Node 1:
 (export seed1="akka.tcp://127.0.0.1:2551"; export seed2="akka.tcp://127.0.0.1:2552"; \
 export host="127.0.0.1"; export port="2551"; ./run.seed.node.sh)
2. Seed Node 2:
 (export seed1="akka.tcp://127.0.0.1:2551"; export seed2="akka.tcp://127.0.0.1:2552"; \
 export host="127.0.0.1"; export port="2552"; ./run.seed.node.sh)
3. Worker Node 1:
 (export seed1="akka.tcp://127.0.0.1:2551"; export seed2="akka.tcp://127.0.0.1:2552"; \
 export host="127.0.0.1"; export port="2553"; ./run.worker.node.sh)
4. Worker Node 2:
 (export seed1="akka.tcp://127.0.0.1:2551"; export seed2="akka.tcp://127.0.0.1:2552"; \
 export host="127.0.0.1"; export port="2554"; ./run.worker.node.sh)
5. Master Node 1:
 (export seed1="akka.tcp://127.0.0.1:2551"; export seed2="akka.tcp://127.0.0.1:2552"; \
 export host="127.0.0.1"; export port="2555"; export queue="amqp://guest:guest@127.0.0.1:5672"; ./run.master.node.sh)

Warning
-------
>Launching seednode, workernode and masternode will produce a **false** Sigar library exception:

  **org.hyperic.sigar.SigarException: no libsigar-universal64-macosx.dylib in java.library.path**

>When Sigar fails to find its native library dependency in java.library.path, it downloads it to ${user.dir}/sigar,
which is configurable in seed.conf, worker.conf and master.conf. See the **native-library-extract-folder** property.

>Sigar then logs this message:

  **Sigar library provisioned: ./sigar/libsigar-universal64-macosx.dylib**

>And, then, all is well.

Logs
----
>See **./logs**

VisualVM
--------
1. Install: ( https://visualvm.java.net/download.html )
2. Plugins: Tools > Plugins ( select mbeans, etc ... )
3. Monitor: MasterNode, WorkerNode and SeedNodes

>**Note** Via VisualVM Applications left tree panel, select Remote > Add Remote Host...,
and enter IP: 10.228.17.133:7099 to monitor Akka cluster nodes on test server dchieva01.

>**Durations** Select the running masternode JVM, MBeans tab and eva.masternode.durations MBean
to view current and average request/response pair durations.
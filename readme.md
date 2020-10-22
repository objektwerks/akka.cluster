Akka Cluster
------------
>Akka cluster project --- using per-request dynamic master-worker teams to yield factorials.
>See ***viewme.pdf*** for details.

Project
-------
>A ***multi-project*** sbt project:

1. cluster
2. core
3. masternode
4. seednode
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
1. Worker Router - A cluster-aware group router containing a dynamic pool of available remote Worker actors. See Broker actor code.

Design
------
>The cluster is architecturally centered around the Broker actor, which resides on the master node and **dynamically**
pulls messages from RabbitMQ via a Queue actor and creates Master actors ***just-in-time*** that delegate work to a
Worker actor on a worker node.

Scheduler
---------
>The Broker actor pulls messages via the Queue actor in the following ways:

1. On MemberUp cluster events
2. On FactorialComputed message
3. Via this scheduler: ```context.system.scheduler.schedule(1 minute, 10 seconds)(runFactorial)```

>If the scheduler discovers (1) ZERO active Master actors **OR** (2) more available Worker actors than active Master
actors, then the Broker actor sends the Queue actor a GetFactorial message.

Timing
------
>The Master actor, created dynamically by the Broker actor, has a time-to-live , which is currently set to:

    ```context.setReceiveTimeout(3 minutes)```

>When the **time-to-live** is exceeded, a Master actor receives a ReceiveTimout message, self-terminates and notifies
the Broker actor, which tells a Queue actor to send a NACK message to RabbitMQ - retaining the origial request on
RabbitMQ for future processing. This value is **TUNABLE**!

Worker Memory
-------------
>In the build.sbt, tune the JVM options as required.

```
lazy val workerNodeSettings = commonSettings ++ packSettings ++ Seq(
  libraryDependencies ++=  akkaDependencies,
  packCopyDependenciesUseSymbolicLinks := false,
  packJvmOpts := Map("worker-node" -> Seq("-server", "-Xss1m", "-Xms1g", "-Xmx32g"))
)
```

RabbitMQ
--------
1. RabbitMQ ( https://www.rabbitmq.com/download.html )
2. RabbitMQ Management Web UI ( https://www.rabbitmq.com/management.html )
3. rabbitmqadmin - https://www.rabbitmq.com/management-cli.html
4. rabbitmqctl - https://www.rabbitmq.com/man/rabbitmqctl.1.man.html
5. web ui - http://http://localhost:15672/  [ user: guest, password: guest ]
6. restart sequence:
    1. rabbitmqctl stop_app
    2. rabbitmqctl reset
    3. rabbitmqctl start_app
    4. rabbitmqctl list_queues name messages_ready messages_unacknowledged
>A local or network instance of RabbitMQ must be available for (1) integration
tests and (2) deployment ( See: Run > Master Node 1 below for details. )

RabbitMQ Homebrew
-----------------
1. brew install rabbitmq
2. brew services start | stop rabbitmq
3. rabbitmq web ui - http://http://localhost:15672/  [ user: guest, password: guest ]

Compile
-------
1. sbt clean compile

Test
----
1. sbt clean test

Integration Test
----------------
1. sbt clean it:test
>View RabbitMQ Web UI at: http://http://localhost:15672/  [ user: guest, password: guest ]

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

Logs
----
>See **./logs**

VisualVM
--------
1. Install: ( https://visualvm.java.net/download.html )
2. Plugins: Tools > Plugins ( select mbeans, etc ... )
3. Monitor: MasterNode, WorkerNode and SeedNodes

>**Durations** Select the running masternode JVM, MBeans tab and objektwerks.masternode.durations MBean
to view current and average request/response pair durations.
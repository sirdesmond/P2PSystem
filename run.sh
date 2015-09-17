java  -Djava.library.path=./sigar -Dakka.cluster.seed-nodes.0=akka.tcp://P2PSystem@127.0.0.1:2551 -cp target/scala-2.11/akka-mini-p2p-scala-assembly-1.0.jar desmond.backend.Main $1 $2 $3 $4


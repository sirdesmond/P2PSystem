package desmond.backend

import java.io._
import java.util.concurrent.TimeUnit
import java.util.{Random, UUID, Scanner}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.contrib.pattern.{ClusterReceptionistExtension, ClusterClient}
import akka.pattern.Patterns
import akka.util.Timeout
import com.typesafe.config.{ConfigFactory}
import scala.collection.mutable
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kofikyei on 9/4/15.
 * The main application that will start server, peer and interactive terminal
 */
object Main extends App{
  var f:Future[KnownSeeders] = _
  var selected_peer:ActorRef = _

  override def main (args: Array[String]) {
    if (args.isEmpty) {
      startServer("127.0.0.1",2551,"server")
      startPeer(Array("0"))
    } else {
      val hostname = args(0)
      val port = args(1).toInt
      if (2000 <= port && port <= 2999)
        startServer(hostname,port,"server")
      else if (3000 <= port && port <= 3999)
        startPeer(args)
      else
        startTerminal(args)
    }
  }

  /**
   * Start server on provided socket
   * @param hostname
   * @param port
   * @param role
   */

  def startServer(hostname:String,port: Int, role: String): Unit = {
    val config =
      ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname").
        withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")).
        withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [$role]")).
        withFallback(ConfigFactory.load())

    val system = ActorSystem("P2PSystem",config)

    system.actorOf(Props[IdxServer],"server")
  }


  /**
   * start peer from provided config
   * @param args
   */

  def startPeer(args:Array[String]): Unit = {
    val config =
      ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=${args(0)}").
        withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${args(1)}")).
        withFallback(ConfigFactory.parseString("akka.cluster.roles = [peer]")).
        withFallback(ConfigFactory.load())

    val system = ActorSystem("P2PSystem",config)


    val f = (s"peer${args(2)}")

    val source_dir = if(args(3).isEmpty) "/tmp/shared" else args(3)



    val peer = system.actorOf(Client.props(source_dir), s"peer${args(2)}")

    ClusterReceptionistExtension(system).registerService(peer)

    //requests simulator

    system.actorOf(Props(classOf[TaskProducer], peer,f), "producer")

  }

  /**
   * Start interactive terminal
   * @param args
   */

  def startTerminal(args:Array[String]) = {
    val reader:BufferedReader = new BufferedReader(new InputStreamReader(System.in))

    val scanner = new Scanner(System.in)
    val r = new Random(1000)

    val config =
      ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=${args(0)}").
        withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${args(1)}")).
        withFallback(ConfigFactory.parseString("akka.cluster.roles = [client]")).
        withFallback(ConfigFactory.load())

    val system = ActorSystem("P2PSystem",config)

    implicit val resolveTimeout = Timeout(10 seconds)

    while (true){
      println("1 - Lookup file")
      println("2 - Exit")

      println()
      print("Choice? : ")

      val line = {
        reader.readLine().toInt
      }

      line match {
        case 1 =>
          print("Enter file name: ")
          val fileName = reader.readLine()
          var peers = mutable.Set.empty[ActorRef]

          val seed = config.getStringList("akka.cluster.seed-nodes").get(0)
          var initialContacts = Set(
            system.actorSelection(s"$seed/user/receptionist"))
          var clusterClient:ActorRef = system.actorOf(ClusterClient.props(initialContacts), s"clusterClient${UUID.randomUUID().toString}")

          var avg_response_time = 0.0


          for{
            i <-1 to 10
          }yield {
            val startTime = System nanoTime()
            f = Patterns.ask(clusterClient,new ClusterClient.Send("/user/server",Lookup(fileName), true),resolveTimeout).mapTo[KnownSeeders]
            Await.result(f,10 minutes)

            val estimatedTime = (System.nanoTime() - startTime)

            avg_response_time += TimeUnit.MILLISECONDS.convert(estimatedTime,TimeUnit.NANOSECONDS)
          }

          avg_response_time = avg_response_time /10

          println()
          println(s"The average response time of lookup was $avg_response_time milliseconds")
          println()
          var s=""
          var count = 0


          f onSuccess {

            case KnownSeeders(seeders) => {
              peers = seeders
              println(s"Peers with the requested file:")

              seeders foreach {
                case sndr =>
                  count += 1
                  println(s"$count -> ${sndr.path}")
              }
            }

            case m => println("received: "+m)
          }

          Thread.sleep(1000)

          if (peers.size > 1) {

            print("Select Peer by number to download file : ")

            val choice = reader.readLine().toInt

            selected_peer = peers.toList(choice - 1)
          } else {
            selected_peer = peers.toList(0)
          }

          print("Enter full path of download directory(default: /tmp/downloads): ")

          val dir =  if (reader.readLine().trim.isEmpty) "/tmp/downloads" else reader.readLine().trim
          println(s"Please wait...")
          println(s"Downloading $fileName from ${selected_peer.path.address}")

           initialContacts = Set(
            system.actorSelection(s"${selected_peer.path.address}/user/receptionist"))

          clusterClient = system.actorOf(ClusterClient.props(initialContacts), s"clusterClient${r.nextInt(2000)}")
          val peer = system.actorOf(Client.props("/tmp/shared/"), s"peer${r.nextInt(2000)}")

           val future = Patterns.ask(clusterClient,
            new ClusterClient.Send(s"/user/${selected_peer.path.name}",
              DownloadFile(peer,fileName, dir), true),resolveTimeout)

          future onFailure {
            case _ => system.stop(peer)
          }

          Thread.sleep(200)
          println("Byeeee!!!!!")

          system.stop(peer)
          system.shutdown()
          System exit(1)

        case 2 =>
          system.shutdown()
          System exit(0)

        case _ => println("unknown input!!")
      }
    }
  }

}

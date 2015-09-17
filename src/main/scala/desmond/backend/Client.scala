package desmond.backend

import java.io._

import akka.actor._
import akka.cluster.{Member, MemberStatus, Cluster}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.event.LoggingReceive
import akka.util.Timeout
import scala.concurrent.duration._


import scala.collection.mutable
import scala.concurrent.Await

/**
 * Created by kofikyei on 9/4/15.
 * Creates Peers
 */

object Client {
  def props(sharedDir:String) = Props(new Client(sharedDir))

}


class Client(sharedDir:String) extends Actor with ActorLogging{
  val servers = mutable.Set.empty[ActorRef]
  val peers = mutable.Map.empty[String,ActorRef]
  var listeners = Set.empty[ActorRef]
  var name = ""

  val cluster = Cluster(context.system)



  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }
  override def postStop(): Unit = cluster.unsubscribe(self)


  /**
   * Add members with server role to this peers list as a tracker
   * @param member
   */
  def register(member: Member): Unit ={

    if (member.hasRole("server")){
      val ref = context.actorSelection(RootActorPath(member.address) / "user" / "server")
      ref ! PeerRegistration
      implicit val resolveTimeout = Timeout(5 seconds)
      val actorRef = Await.result(ref.resolveOne(), resolveTimeout.duration)
      servers += actorRef
    }
  }

  /**
   * handle expected messages
   * @return
   */
  override def receive: Receive = LoggingReceive{
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)

    case Register(fileName) =>
      listeners += sender
      log.info(s"informing server...$sender")
      servers foreach(_ ! Register(fileName))

    case ServerError(errMsg) =>
      log.error(s"ERROR from server ${sender()}: $errMsg")

    case Lookup(name) =>
      this name = name
      log.info(s"informing server to lookup file...$sender")
      servers foreach(_ ! Lookup(name))

    case DownloadFile(sender,filename,dir) =>
      log.debug("Initiating download...")
      new File(sharedDir).mkdirs
      val filePath = new File(sharedDir+"/",filename)

      val buffer:Array[Byte] = new Array[Byte](filePath.length().toInt)
      val input:BufferedInputStream = new BufferedInputStream(new FileInputStream(filePath))
      input read(buffer,0,buffer.length)
      input.close()
      sender ! DownloadSuccess(buffer,filename,dir)

    case DownloadSuccess(data,filename,dir) =>
      new File(dir).mkdirs
      val path = dir +"/"+ filename
      val  f = new File(path)
      val out: BufferedOutputStream = new BufferedOutputStream((new FileOutputStream(f)))
      out.write(data,0,data.length)
      println("file download complete!!")
      out close

    case m @ SuccessfullyAdded(filename) =>
      listeners.foreach(_ ! m)

    case e =>
        log.debug(s"$e")
  }

}

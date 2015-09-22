package desmond.backend

import java.io._
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.{Member, MemberStatus, Cluster}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.event.LoggingReceive
import akka.pattern.Patterns
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


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
  var total_response_time= 0.0
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
   * Gets the list of files in a directory
   * @param dir
   * @return
   */
  def getListOfFiles(dir:String):List[File]={
    val d = new File(dir)
    if(d.exists() && d.isDirectory){
      d.listFiles.filter(_.isFile).toList
    }else{
      List[File]()
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

    case Register(path) =>
      listeners += sender

      log.info(s"${self.path.name} registering files in $path")

      getListOfFiles(sharedDir).foreach(m => {
        servers.head !  Register(m.getName())
      })
      //benchmark purpose
      /*implicit val resolveTimeout = Timeout(30 seconds)

      log.info(s"${self.path.name} registering files in $path")
      var i = 0

       getListOfFiles(sharedDir).foreach(m =>{
         i+=1
         val startTime = System nanoTime()
         val f = Patterns.ask(servers.head,Register(m.getName()),resolveTimeout)
         Await.result(f,resolveTimeout.duration)

         val estimatedTime = (System.nanoTime() - startTime)

         total_response_time += TimeUnit.MILLISECONDS.convert(estimatedTime,TimeUnit.NANOSECONDS)

         f onSuccess{
           case m @ SuccessfullyAdded(filename) =>
             if(i == 1000) {
               log.info(s"total response time "+total_response_time)
               log.info(s"average registration time was ${total_response_time/i} milliseconds")
               listeners.foreach(_ ! m)
             }
         }
       })*/



    case ServerError(errMsg) =>
      log.error(s"ERROR from server ${sender()}: $errMsg")

    case Lookup(name) =>
      this name = name
      log.info(s"informing server to lookup file...$sender")
      servers foreach(_ ! Lookup(name))

    case DownloadFile(sender,filename,dir) =>
      log.info("Initiating download...")
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
      log.info("file download complete!!")
      out close

    case m @ SuccessfullyAdded(filename) => listeners.foreach(_ ! m)

    case e => log.debug(s"$e")
  }

}

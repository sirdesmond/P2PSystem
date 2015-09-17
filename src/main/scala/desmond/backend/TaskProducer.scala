package desmond.backend

import java.util.UUID
import java.io._

import akka.actor._
import scala.concurrent.duration._
import scala.io.Source

/**
 * Created by kofikyei on 9/13/15.
 * This guy reads files peer1,peer2 and peer3 and registers peers
 */

object TaskProducer{
  case object Task

  def props(): Props =
    Props(classOf[TaskProducer])
}
class TaskProducer(peer:ActorRef,path:String) extends Actor with ActorLogging{
  import context.dispatcher
  import TaskProducer._

  def scheduler = context.system.scheduler
  def nextTaskId(): String = UUID.randomUUID().toString
  var n = 0

  override def preStart():Unit= {
    scheduler.scheduleOnce(5.seconds,self,Task)
  }

  override def postRestart(reason: Throwable): Unit = ()

  /**
   * handle expected messages
   * @return
   */
  override def receive: Receive = {
    case KnownSeeders(seeders) =>
      var s = ""
      seeders foreach {
        case sndr=>
          s+= s"${sndr.path.name}:${sndr}\t"
      }
      log.debug(s"Peers with the requested file ->$s")

    case DownloadSuccess(data,filename,dir) =>
      val path = dir + filename
      val  f = new File(path)
      val out: BufferedOutputStream = new BufferedOutputStream((new FileOutputStream(f)))
      out.write(data,0,data.length)
      println("file download complete!!")
      out close

    case Task =>
      n += 1
      log.info("Produced task: {}", n)


      log.debug(s"reading file $path")
      val source = Source.fromFile(path)
      val lines = try source.mkString finally source.close()

      log.debug(lines)
      val files = lines.split(",")
      files.foreach(m =>{
        scheduler.scheduleOnce(1.second,peer,Register(m))
       })


     /* //lookup
      scheduler.scheduleOnce(5.seconds,peer,Lookup(s"file$n"))

      //download
      if(peer.path.name equals("peer2")){

        val peer1 =  context.system.actorSelection("akka.tcp://P2PSystem@192.168.2.10:3001/user/peer1")

        val peer2 =  context.system.actorSelection("akka.tcp://P2PSystem@192.168.2.9:3002/user/peer2")
        implicit val resolveTimeout = Timeout(5 seconds)
        val peer1_ref = Await.result(peer1.resolveOne(), resolveTimeout.duration)
        val peer2_ref = Await.result(peer2.resolveOne(), resolveTimeout.duration)

        Thread.sleep(1000)
        peer2_ref !    scheduler.scheduleOnce(5.seconds,peer,peer2_ref ! DownloadFileFrom(peer1_ref,"file1","/tmp/downloads/"))
      }*/


  }

}

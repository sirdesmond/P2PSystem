package desmond.backend

import java.util.UUID
import java.io._
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.Patterns
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

/**
 * Created by kofikyei on 9/13/15.
 * This guy initiates Register Operation
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
  override def preStart():Unit= {
    scheduler.scheduleOnce(5.seconds,self,Task)
  }

  override def postRestart(reason: Throwable): Unit = ()

  /**
   * handle expected messages
   * @return
   */

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
  override def receive: Receive = {

    case Task =>
      implicit val resolveTimeout = Timeout(30 seconds)
      scheduler.scheduleOnce(0.second,peer,Register(path))
  }

}

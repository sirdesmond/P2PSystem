package desmond.backend

/**
 * Created by kofikyei on 9/4/15.
 */

import java.util.NoSuchElementException

import akka.actor.{Terminated, Actor, ActorLogging, ActorRef}
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.event.LoggingReceive

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
 * Created by kofikyei on 9/13/15.
 * This is the Index Server
 * Stores peers and file paths
 */
class IdxServer extends Actor with ActorLogging {

  val db = TrieMap.empty[String,mutable.Set[ActorRef]]
  log.info(s"Initializing server...$self")

  var peers = IndexedSeq.empty[ActorRef]

  ClusterReceptionistExtension(context.system).registerService(self)

  override def preStart(): Unit = {
    log.debug(s"my path is ${context.self.path}")
  }

  /**
   * add a peer as a seeder of a file
   * @param fileName
   * @param sndr
   */
  def addSeeder(fileName:String,sndr:ActorRef)={

    var seeders = mutable.Set.empty[ActorRef]

    try{
       seeders = db(fileName)

    }catch{
      case _: NoSuchElementException =>
    }finally {
      println(s"adding new seeder for file $fileName")
      seeders+=sndr
      db put (fileName,seeders)
    }
  }

  /**
   * handle expected messages
   * @return
   */
  override def receive: Receive = LoggingReceive{

    case PeerRegistration if !peers.contains(sender()) =>
      log.debug("new member!!")
      context watch sender()
      peers = peers :+ sender()
      sender ! self

    case Terminated(a) =>
      peers = peers.filterNot(_ == a)


    case Register(fileName) =>
      def aware_of_file = db contains fileName
      def no_change = sender ! ServerError(s"You are already seeding this file")
      def success = sender !  SuccessfullyAdded(fileName)

      addSeeder(fileName,sender)
      success


    case Lookup(fileName) =>
      //if I'm aware of file send list of path and ref pairs to sender
      log.debug(s"looking up on server: $fileName")
      if(db contains fileName){
        sender forward KnownSeeders(db(fileName))
      }else{
        sender ! ServerError(s"unknown file with name: $fileName")
      }

  }


}

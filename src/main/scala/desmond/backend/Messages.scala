package desmond.backend

import akka.actor.ActorRef

import scala.collection.mutable

/**
 * Created by kofikyei on 9/4/15.
 * All the messages that will flow in and out of the cluster
 */

sealed trait Event
case class Register(fileName:String) extends Event
case class Lookup(fileName:String) extends Event
case class DownloadFile(peer:ActorRef,fileName:String,downloadDir:String)
case class DownloadFileFrom(peer:ActorRef,fileName:String,downloadDir:String)
case class DownloadSuccess(data:Array[Byte],fileName:String,downloadDir:String)
case class ServerError(msg:String)
case class SuccessfullyAdded(fileName:String)
case class KnownSeeders(seeders:mutable.Set[ActorRef])
case object PeerRegistration
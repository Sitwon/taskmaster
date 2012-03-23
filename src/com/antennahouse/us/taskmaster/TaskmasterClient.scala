package com.antennahouse.us.taskmaster

import Messages._

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory

import java.io.File
import ahrts.app.common.VisualComparison._
import ahrts.app.common.config.Properties

object TaskmasterClient {
  var taskmasterServiceActor: ActorRef = null
  var system: ActorSystem = _

  def main(args: Array[String]) {
    var port = 2553
    if (args.length < 3) {
      System.err.println("Usage: TaskmasterClient <client-number> <client-IP> <server-IP>")
      System exit 1
    }
    try {
      val client_number = Integer.parseInt(args(0))
      port += client_number
    } catch {
      case e: NumberFormatException =>
        println("Argument was not a number.")
        System exit 1
    }
    Properties.load(new File("ahrts.properties")
    val config = ConfigFactory.parseString("""
        akka {
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            transport = "akka.remote.netty.NettyRemoteTransport"
            netty {
              hostname = "%s"
              port = "%s"
            }
          }
        }
        """.format(args(1), port))
    system = ActorSystem("TaskmasterClientApplication", ConfigFactory.load(config))
    val actor = system.actorOf(Props[TaskmasterClientActor], "taskmaster-client")
    taskmasterServiceActor = system.actorFor(
        "akka://TaskmasterServiceApplication@"+args(2)+":2552/user/taskmaster-service")
    actor ! JobRequest
  }

  class TaskmasterClientActor extends Actor {
    def receive = {
      case JobsFinished =>
        println("No more jobs to do.")
        system.shutdown()
      case Job(data) =>
        // Process data
        println("Processing " + data._1 + " and " + data._2)
        try {
          compare(data._1, data._2)
        } catch {
          case e: Exception =>
            println("An error occurred: " + e.getMessage())
            e.printStackTrace()
        }
        sender ! JobResult(data)
        requestAJob()
      case JobRequest =>
        println("received JobRequest.")
        requestAJob()
    }

    def requestAJob() {
      println("Requesting a Job.")
      taskmasterServiceActor ! JobRequest
    }
  }
}


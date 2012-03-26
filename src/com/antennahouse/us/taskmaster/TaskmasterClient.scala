package com.antennahouse.us.taskmaster

import Messages._

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory

import java.io.File
import ahrts.app.common.VisualComparison._
import ahrts.common.config.Properties

object TaskmasterClient {
  private var taskmasterServiceActor: ActorRef = null
  private var system: ActorSystem = _

  def main(args: Array[String]) {
    var port = 2553
    if (args.length < 2) {
      System.err.println("Usage: TaskmasterClient <client-IP> <server-IP>")
      System exit 1
    }
    Properties.load(new File("ahrts.properties"))
    val actor = startClient(args(0), args(1))
    actor ! JobRequest
  }

  def startClient(ip: String, server: String) = {
    val config = ConfigFactory.parseString("""
        akka {
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            transport = "akka.remote.netty.NettyRemoteTransport"
            netty {
              hostname = "%s"
              port = "0"
            }
          }
        }
        """.format(ip))
    system = ActorSystem("TaskmasterClientApplication", ConfigFactory.load(config))
    taskmasterServiceActor = system.actorFor(
        "akka://TaskmasterServiceApplication@"+server+":2552/user/taskmaster-service")
    system.actorOf(Props[TaskmasterClientActor], "taskmaster-client")
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


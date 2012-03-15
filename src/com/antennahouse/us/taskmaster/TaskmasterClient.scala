package com.antennahouse.us.taskmaster

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory

import StreamGobbler._

object TaskmasterClient {
  var taskmasterServiceActor: ActorRef = null

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
    val system = ActorSystem("TaskmasterClientApplication", ConfigFactory.load(config))
    val actor = system.actorOf(Props[TaskmasterClientActor], "taskmaster-client")
    taskmasterServiceActor = system.actorFor("akka://TaskmasterServiceApplication@"+args(2)+":2552/user/taskmaster-service")
    actor ! JobRequest
  }

  class TaskmasterClientActor extends Actor {
    def receive = {
      case JobsFinished =>
        println("No more jobs to do.")
        Runtime.getRuntime().halt(0)
      case Job(data) =>
        // Process data
        val proc = Runtime.getRuntime().exec(Array("/bin/sh", "/home/antenna/ahrts-dist/compare.sh", data._1.getAbsolutePath(), data._2.getAbsolutePath()))
        printGobbler(proc.getErrorStream())
        printGobbler(proc.getInputStream())
        println("Processing " + data._1 + " and " + data._2)
        proc.waitFor()
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


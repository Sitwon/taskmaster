package com.antennahouse.us.taskmaster

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

object TaskmasterClient {
  var taskmasterServiceActor: ActorRef = null

  def main(args: Array[String]) {
    var port = 2553
    if (args.length > 0) {
      try {
        val client_number = Integer.parseInt(args(0))
        port += client_number
      } catch {
        case e: NumberFormatException =>
          println("Argument was not a number.")
          System exit 1
      }
    }
    remote.start(args(1), port)
    taskmasterServiceActor = remote.actorFor("taskmaster-service", args(2), 2552)
    val localActor = actorOf[TaskmasterClientActor]
    localActor.start()
    localActor ! JobRequest
  }

  class TaskmasterClientActor extends Actor {
    def receive = {
      case JobsFinished =>
        println("No more jobs to do.")
        Runtime.getRuntime().halt(0)
      case Job(data) =>
        // Process data
        val proc = Runtime.getRuntime().exec(Array("/bin/sh", "-c", "/home/antenna/ahrts-dist/compare.sh", data._1.getAbsolutePath(), data._2.getAbsolutePath()))
        println("Processing " + data._1 + " and " + data._2)
        proc.waitFor()
        self reply JobResult(data)
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


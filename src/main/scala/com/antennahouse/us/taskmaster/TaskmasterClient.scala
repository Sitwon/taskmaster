package com.antennahouse.us.taskmaster

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

object TaskmasterClient {
  var taskmasterServiceActor: ActorRef = null

  def main(args: Array[String]) {
    remote.start("localhost", 2553)
    taskmasterServiceActor = remote.actorFor("taskmaster-service", "localhost", 2552)
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
        println("Processing " + data._1 + " and " + data._2)
        Thread sleep 1000
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


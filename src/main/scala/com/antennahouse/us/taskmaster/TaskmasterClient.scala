package com.antennahouse.us.taskmaster

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

import java.io._

object TaskmasterClient {
  var taskmasterServiceActor: ActorRef = null

  def main(args: Array[String]) {
    var port = 2553
    if (args.length < 3) { System exit 1 }
    try {
      val client_number = Integer.parseInt(args(0))
      port += client_number
    } catch {
      case e: NumberFormatException =>
        println("Argument was not a number.")
        System exit 1
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
        val proc = Runtime.getRuntime().exec(Array("/bin/sh", "/home/antenna/ahrts-dist/compare.sh", data._1.getAbsolutePath(), data._2.getAbsolutePath()))
        (new StreamGobbler(proc.getErrorStream())).start()
        (new StreamGobbler(proc.getInputStream())).start()
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

  class StreamGobbler(is: InputStream) extends Thread {
    override def run() {
      try {
        val reader = new BufferedReader(new InputStreamReader(is))
        var line: String = reader.readLine()
        while (line != null) {
          println("OUTPUT: " + line)
          line = reader.readLine()
        }
      } catch {
        case e: Exception =>
          println(e.getMessage())
      }
    }
  }
}


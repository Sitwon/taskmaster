package com.antennahouse.us.taskmaster

import akka.actor.Actor
import akka.actor.Actor._
import java.io.{File, FilenameFilter}

sealed trait TaskMessage
case object JobRequest extends TaskMessage
case object JobsFinished extends TaskMessage
case class Job(data: (File,File)) extends TaskMessage
case class JobResult(data: (File,File)) extends TaskMessage

object TaskmasterService {
  var compare_list: List[(File, File)] = Nil

  def main(args: Array[String]) {
    if (args.length < 1) {
      printUsage()
      System exit 1
    }
    val input_dir = new File(args(0))
    if (!input_dir.isDirectory) {
      printUsage()
      System exit 2
    }
    val test_docs = input_dir.listFiles(new FilenameFilter() {
        def accept(dir: File, name: String): Boolean = {
          return name.toLowerCase().endsWith(".xml")
        }
      })
    var test_docs_list = test_docs.toList
    test_docs_list = test_docs_list sortWith {_.toString() < _.toString()}
    var even = false
    var odd: File = null
    for ( file <- test_docs_list ) {
      if (!even) {
        odd = file
        even = true
      } else {
        println(odd.toString() + " " + file.toString())
        compare_list = (odd, file) :: compare_list
        even = false
      }
    }
    compare_list = compare_list.reverse
    remote.start("0.0.0.0", 2552)
    remote.registerPerSession("taskmaster-service", actorOf[TaskmasterServiceActor])
  }

  def printUsage() {
    println("You're doing it wrong.")
  }

  class TaskmasterServiceActor extends Actor {
    var sent = 0
    var received = 0
    var total = 0

    override
    def preStart() {
      total = compare_list.length
    }

    def receive = {
      case JobRequest =>
        println("Got a JobRequest.")
        if (compare_list == Nil) {
          println("No more jobs.")
          self reply JobsFinished
        } else {
          println("Sending a job.")
          self reply Job(compare_list.head)
          compare_list = compare_list.tail
          sent += 1
          println("Sent: " + sent + " of " + total)
        }
      case JobResult(data) =>
        println("Received a JobResult.")
        received += 1
        println("Received: " + received + " of " + total)
    }
  }
}


package com.antennahouse.us.taskmaster

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import com.typesafe.config.ConfigFactory
import java.io.{File, FilenameFilter}

sealed trait TaskMessage
case object JobRequest extends TaskMessage
case object JobsFinished extends TaskMessage
case class Job(data: (File,File)) extends TaskMessage
case class JobResult(data: (File,File)) extends TaskMessage

object TaskmasterService {
  var compare_list: List[(File, File)] = Nil
  var sent = 0
  var received = 0
  var total = 0

  def main(args: Array[String]) {
    if (args.length < 2) {
      printUsage()
      System exit 1
    }
    val input_dir = new File(args(1))
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
    total = compare_list.length
    val config = ConfigFactory.parseString("""
        akka {
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            transport = "akka.remote.netty.NettyRemoteTransport"
            netty {
              hostname = "%s"
              port = 2552
            }
          }
        }
        """.format(args(0)))
    val system = ActorSystem("TaskmasterServiceApplication", ConfigFactory.load(config))
    val actor = system.actorOf(Props[TaskmasterServiceActor], "taskmaster-service")
  }

  def printUsage() {
    System.err.println("Usage: TaskmasterService <server-IP> <input-dir>")
  }

  class TaskmasterServiceActor extends Actor {
    def receive = {
      case JobRequest =>
        println("Got a JobRequest.")
        if (compare_list == Nil) {
          println("No more jobs.")
          sender ! JobsFinished
        } else {
          println("Sending a job.")
          sender ! Job(compare_list.head)
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


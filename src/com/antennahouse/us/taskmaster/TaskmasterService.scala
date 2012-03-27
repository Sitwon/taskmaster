package com.antennahouse.us.taskmaster

import Messages._

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import com.typesafe.config.ConfigFactory
import java.io.{File, FilenameFilter}
import scala.collection.mutable.Queue

import ahrts.common.config.{Properties=>props}
import ahrts.app.common.VisualComparison.compare

object TaskmasterService {
  def main(args: Array[String]) {
    props.load(new File("ahrts.properties"))
    if (args.length < 3) {
      printUsage()
      System exit 1
    }
    val input_dir = new File(props.testOutputDir)
    if (!input_dir.isDirectory) {
      System exit 2
    }

    val tms = new TaskmasterService(args(0), 2552)

    val test_doc_dirs = input_dir.listFiles(new FilenameFilter() {
        def accept(dir: File, name: String): Boolean = {
          if (!(new File(dir, name)).isDirectory) return false
          if (args.length == 3) {
            true
          } else {
            args.drop(3).contains(name)
          }
        }
      })

    val regA = ("(.*)-" + args(1) + """.xml""").r
    val regB = ("(.*)-" + args(2) + """.xml""").r

    test_doc_dirs foreach {
      test_dir: File =>
      val test_docs = test_dir.listFiles(new FilenameFilter() {
        def accept(dir: File, name: String): Boolean = {
          val test_name = dir.getName()
          name match {
            case regA(test_name) => true
            case regB(test_name) => true
            case _ => false
          }
        }
      }).toList
      if (test_docs.length == 2)
        tms.addJob((test_docs(0), test_docs(1))) { compare(test_docs(0), test_docs(1)) }
    }
  }

  def printUsage() {
    System.err.println("Usage: TaskmasterService <server-IP> <engine-A> <engine-B> [test-name ...]")
  }

  private class TaskmasterServiceActor extends Actor {
    private val job_queue = Queue[Job]()
    private var sent = 0
    private var received = 0

    def receive = {
      case JobRequest =>
        println("Got a JobRequest.")
        if (job_queue.isEmpty) {
          println("No more jobs.")
          sender ! JobsFinished
        } else {
          println("Sending a job.")
          sender ! job_queue.head
          job_queue enqueue job_queue.dequeue
          sent += 1
          println("Sent: " + sent)
          println("Remaining: " + job_queue.length)
        }
      case JobResult(data) =>
        println("Received a JobResult.")
        job_queue dequeueFirst {
          _ match {
            case Job(`data`, _) => true
            case _ => false
          }
        }
        received += 1
        println("Received: " + received + " of " + sent)
      case AddJob(job) =>
        job_queue enqueue job
    }
  }
}

class TaskmasterService(ip: String, port: Int) {
  private val actor: ActorRef = startService

  private def startService() = {
    val config = ConfigFactory.parseString("""
        akka {
          actor {
            provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
            transport = "akka.remote.netty.NettyRemoteTransport"
            netty {
              hostname = "%s"
              port = %d
            }
          }
        }
        """.format(ip, port))
    val system = ActorSystem("TaskmasterServiceApplication", ConfigFactory.load(config))
    system.actorOf(Props[TaskmasterService.TaskmasterServiceActor], "taskmaster-service")
  }

  def addJob(data: Any)(task: => Unit) { actor ! AddJob(Job(data, () => task)) }
}

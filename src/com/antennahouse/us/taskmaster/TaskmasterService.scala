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
  private var actor: ActorRef = _
  private var sent = 0
  private var received = 0

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

    actor = startService(args(0))

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
      if (test_docs.length == 2) addJob(test_docs(0), test_docs(1))
    }
  }

  def startService(ip: String) = {
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
        """.format(ip))
    val system = ActorSystem("TaskmasterServiceApplication", ConfigFactory.load(config))
    system.actorOf(Props[TaskmasterServiceActor], "taskmaster-service")
  }

  def addJob(a: File, b: File) { actor ! AddJob((a,b)) }

  def genAddJob(a: File, b: File) {
    actor ! GenAddJob(GenJob((a,b), () => compare(a, b)))
  }

  def printUsage() {
    System.err.println("Usage: TaskmasterService <server-IP> <engine-A> <engine-B> [test-name ...]")
  }

  class TaskmasterServiceActor extends Actor {
    private val compare_queue: Queue[(File, File)] = Queue[(File, File)]()
    private val gen_queue = Queue[GenJob]()

    def receive = {
      case JobRequest =>
        println("Got a JobRequest.")
        if (compare_queue.isEmpty) {
          println("No more jobs.")
          sender ! JobsFinished
        } else {
          println("Sending a job.")
          sender ! Job(compare_queue.head)
          compare_queue enqueue compare_queue.dequeue
          sent += 1
          println("Sent: " + sent)
          println("Remaining: " + compare_queue.length)
        }
      case JobResult(data) =>
        println("Received a JobResult.")
        compare_queue dequeueFirst { _ == data }
        received += 1
        println("Received: " + received + " of " + sent)
      case AddJob(data) =>
        compare_queue enqueue data
      case GenJobRequest =>
        println("Got a GenJobRequest.")
        if (gen_queue.isEmpty) {
          println("No more jobs.")
          sender ! GenJobsFinished
        } else {
          println("Sending a job.")
          sender ! gen_queue.head
          gen_queue enqueue gen_queue.dequeue
          sent += 1
          println("Sent: " + sent)
          println("Remaining: " + gen_queue.length)
        }
      case GenJobResult(data) =>
        println("Received a GenJobResult.")
        gen_queue dequeueFirst {
          _ match {
            case GenJob(`data`, _) => true
            case _ => false
          }
        }
        received += 1
        println("Received: " + received + " of " + sent)
      case GenAddJob(job) =>
        gen_queue enqueue job
    }
  }
}


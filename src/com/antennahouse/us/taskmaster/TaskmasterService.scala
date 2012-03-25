package com.antennahouse.us.taskmaster

import Messages._

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import com.typesafe.config.ConfigFactory
import java.io.{File, FilenameFilter}

import ahrts.common.config.{Properties=>props}

object TaskmasterService {
  private var actor: ActorRef = _
  private var compare_list: List[(File, File)] = Nil
  var sent = 0
  var received = 0

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
    val test_docs_list = (List[File]() /: test_doc_dirs) (_ ++ _.listFiles(new FilenameFilter() {
        def accept(dir: File, name: String): Boolean = {
          val test_name = dir.getName()
          name match {
            case regA(test_name) => true
            case regB(test_name) => true
            case _ => false
          }
        }
      }))
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
    startService(args(0))
  }

  def startService(ip: String) {
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
    actor = system.actorOf(Props[TaskmasterServiceActor], "taskmaster-service")
  }

  def addJob(a: File, b: File) { actor ! AddJob((a,b)) }

  def printUsage() {
    System.err.println("Usage: TaskmasterService <server-IP> <engine-A> <engine-B> [test-name ...]")
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
          compare_list = compare_list.tail :+ compare_list.head
          sent += 1
          println("Sent: " + sent)
          println("Remaining: " + compare_list.length)
        }
      case JobResult(data) =>
        println("Received a JobResult.")
        if (compare_list exists { _ == data }) {
          compare_list = compare_list filterNot { _ == data }
        }
        received += 1
        println("Received: " + received + " of " + sent)
      case AddJob(data) =>
        compare_list :+= data
    }
  }
}


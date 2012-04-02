package com.antennahouse.us.taskmaster

import Messages._

import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import akka.actor.Actor._
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.Queue

object TaskmasterService {
  def main(args: Array[String]) {
    if (args.length < 1) {
      println("Usage: TaskmasterService <ip-address>")
      System exit 1
    }
    new TaskmasterService(args(0), 2552)
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

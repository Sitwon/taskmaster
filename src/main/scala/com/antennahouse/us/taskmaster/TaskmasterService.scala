package com.antennahouse.us.taskmaster

import akka.actor.Actor
import akka.actor.Actor._

class TaskmasterServiceActor extends Actor {
  def receive = {
    case "Hello" =>
        println("Received 'Hello'.")
        self.reply("World")
  }
}

object TaskmasterService {
  def main(args: Array[String]) {
    remote.start("localhost", 2552)
    remote.registerPerSession("taskmaster-service", actorOf[TaskmasterServiceActor])
  }
}


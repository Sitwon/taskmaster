package com.antennahouse.us.taskmaster

import akka.actor.Actor._

object TaskmasterClient {
  def main(args: Array[String]) {
    val actor = remote.actorFor("taskmaster-service", "localhost", 2552)
    println("Sending 'Hello'.")
    val result = (actor ? "Hello").as[String]
    println("Received: " + result)
    System exit 0
  }
}


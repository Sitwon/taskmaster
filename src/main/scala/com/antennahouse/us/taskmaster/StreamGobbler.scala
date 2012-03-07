package com.antennahouse.us.taskmaster

import java.io._

class StreamGobbler(is: InputStream) extends Thread {
  private var lines: List[String] = Nil

  override def run() {
    try {
      val reader = new BufferedReader(new InputStreamReader(is))
      var line: String = reader.readLine()
      while (line != null) {
        perLine(line)
        line = reader.readLine()
      }
    } catch {
      case e: Exception =>
        println(e.getMessage())
    }
  }

  def perLine(line: String) { lines ::= line }

  def getLines = { lines.reverse }
}

object StreamGobbler {
  def streamGobbler(is: InputStream): List[String] = {
    val sg = new StreamGobbler(is)
    sg.start()
    sg.join()
    sg.getLines
  }

  def printGobbler(is: InputStream) {
    (new StreamGobbler(is) {
        override def perLine(line: String) {
          println("OUTPUT: " + line)
        }
      }).start()
  }
}

package com.antennahouse.us.taskmaster

import java.io.File

object Messages {
  sealed trait TaskMessage
  case object JobRequest extends TaskMessage
  case object JobsFinished extends TaskMessage

  case class Job(data: (File,File)) extends TaskMessage
  case class JobResult(data: (File,File)) extends TaskMessage
  case class AddJob(data: (File,File)) extends TaskMessage

  case class GenJob[D,R](data: D, task: D => R) extends TaskMessage
  case class GenJobResult[D,R](data: D, result: R) extends TaskMessage
  case class GenAddJob(job: GenJob[_,_]) extends TaskMessage
}


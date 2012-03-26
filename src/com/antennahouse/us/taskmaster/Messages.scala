package com.antennahouse.us.taskmaster

import java.io.File

object Messages {
  sealed trait TaskMessage

  case object JobRequest extends TaskMessage
  case object JobsFinished extends TaskMessage

  case class Job(data: Any, task: () => Unit) extends TaskMessage
  case class JobResult(data: Any) extends TaskMessage
  case class AddJob(job: Job) extends TaskMessage
}


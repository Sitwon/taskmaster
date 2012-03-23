package com.antennahouse.us.taskmaster

import java.io.File

object Messages {
  sealed trait TaskMessage
  case object JobRequest extends TaskMessage
  case object JobsFinished extends TaskMessage
  case class Job(data: (File,File)) extends TaskMessage
  case class JobResult(data: (File,File)) extends TaskMessage
}


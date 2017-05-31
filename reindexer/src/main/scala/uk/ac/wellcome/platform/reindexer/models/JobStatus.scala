package uk.ac.wellcome.platform.reindexer.models

import akka.agent.Agent
import uk.ac.wellcome.utils.GlobalExecutionContext.context

object JobStatus extends Enumeration {
  type JobStatus = Value

  val Init = Value("initialised")
  val Working = Value("working")
  val Success = Value("success")
  val Failure = Value("failure")
}

case class ReindexStatus(state: JobStatus.Value,
                         percentComplete: Option[Float]) {
  def toMap =
    Map("state" -> state.toString,
        "percent" -> percentComplete.getOrElse(0F).toString)

}
object ReindexStatus {
  private val initState = ReindexStatus(JobStatus.Init, None)

  val agent: Agent[ReindexStatus] = Agent[ReindexStatus](initState)

  def currentStatus: ReindexStatus = agent.get()

  def init(): Unit = agent.send(ReindexStatus(JobStatus.Init, None))

  def work(percent: Float): Unit =
    agent.send(ReindexStatus(JobStatus.Working, Some(percent)))

  def succeed(): Unit =
    agent.send(ReindexStatus(JobStatus.Success, currentStatus.percentComplete))

  def fail(): Unit =
    agent.send(ReindexStatus(JobStatus.Failure, currentStatus.percentComplete))
}

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
                         recordsProcessed: Int = 0,
                         batch: Int = 0) {
  def toMap =
    Map("state" -> state.toString,
        "recordsProcessed" -> s"$recordsProcessed",
        "batch" -> s"$batch")

}
object ReindexStatus {
  private val initState = ReindexStatus(JobStatus.Init)
  private val agent: Agent[ReindexStatus] = Agent[ReindexStatus](initState)

  def currentStatus: ReindexStatus = agent.get()

  def init(): Unit = agent.send(ReindexStatus(JobStatus.Init))

  def progress(units: Int, batch: Int = 0): Unit = {
    agent.send(currentStatus => {
      val currentCount = currentStatus.recordsProcessed + units
      val currentBatch = currentStatus.batch + batch
      ReindexStatus(JobStatus.Working, currentCount, currentBatch)
    })
  }

  def succeed(): Unit =
    agent.send(
      currentStatus =>
        ReindexStatus(JobStatus.Success,
                      currentStatus.recordsProcessed,
                      currentStatus.batch))

  def fail(): Unit =
    agent.send(
      currentStatus =>
        ReindexStatus(JobStatus.Failure,
                      currentStatus.recordsProcessed,
                      currentStatus.batch))
}

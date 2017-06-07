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
                         recordsProcessed: Option[Int] = None,
                         batch: Option[Int] = None
                        ) {
  def toMap =
    Map("state" -> state.toString,
        "recordsProcessed" -> recordsProcessed.getOrElse(0).toString,
        "batch" -> batch.getOrElse(1).toString
    )

}
object ReindexStatus {
  private val initState = ReindexStatus(JobStatus.Init)
  private val agent: Agent[ReindexStatus] = Agent[ReindexStatus](initState)

  def currentStatus: ReindexStatus = agent.get()

  def init(): Unit = agent.send(ReindexStatus(JobStatus.Init))

  private def zeroIsNone(n: Int): Option[Int] = n match {
    case i if i > 0 => Some(i)
    case _ => None
  }

  def progress(units: Int, batch: Int = 0): Unit = {
    val currentCount = zeroIsNone(currentStatus.recordsProcessed.getOrElse(0) + units)
    val currentBatch = zeroIsNone(currentStatus.batch.getOrElse(0) + batch)

    agent.send(ReindexStatus(JobStatus.Working,  currentCount, currentBatch))
  }

  def succeed(): Unit =
    agent.send(ReindexStatus(JobStatus.Success, currentStatus.recordsProcessed, currentStatus.batch))

  def fail(): Unit =
    agent.send(ReindexStatus(JobStatus.Failure, currentStatus.recordsProcessed, currentStatus.batch))
}

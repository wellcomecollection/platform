package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sqs.SQSConfig

import scala.concurrent.duration._

trait SqsConfigConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val sqsQueueUrl: ScallopOption[String] = opt[String](required = true)
  private val sqsWaitTimeSeconds = opt[Int](required = true, default = Some(20))
  private val sqsMaxMessages = opt[Int](required = true, default = Some(10))
  private val sqsParallelism = opt[Int](required = true, default = Some(10))

  verify()

  val sqsConfig = SQSConfig(
    queueUrl = sqsQueueUrl(),
    waitTime = sqsWaitTimeSeconds() seconds,
    maxMessages = sqsMaxMessages(),
    parallelism = sqsParallelism()
  )
}

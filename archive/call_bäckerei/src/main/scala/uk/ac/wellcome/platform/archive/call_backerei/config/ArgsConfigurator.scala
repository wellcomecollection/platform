package uk.ac.wellcome.platform.archive.call_backerei.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBackereiConfig
import uk.ac.wellcome.platform.archive.common.config.{CloudWatchClientConfigurator, MetricsConfigConfigurator, SnsClientConfigurator, SqsClientConfigurator}

import scala.concurrent.duration._



class ArgsConfigurator(val arguments: Seq[String])
  extends ScallopConf(arguments)
  with CloudWatchClientConfigurator
  with MetricsConfigConfigurator
  with SnsClientConfigurator
  with SqsClientConfigurator {

  val topicArn: ScallopOption[String] = opt[String](required = true)
  val sqsQueueUrl: ScallopOption[String] = opt[String](required = true)
  val sqsWaitTimeSeconds = opt[Int](required = true, default = Some(20))
  val sqsMaxMessages = opt[Int](required = true, default = Some(10))
  val sqsParallelism = opt[Int](required = true, default = Some(10))



  verify()

  val sqsConfig = SQSConfig(
    sqsQueueUrl(),
    sqsWaitTimeSeconds() seconds,
    sqsMaxMessages(),
    sqsParallelism()
  )

  val snsConfig = SNSConfig(
    topicArn(),
  )

  val appConfig = CallBackereiConfig(
    cloudwatchClientConfig = cloudwatchClientConfig,
    sqsClientConfig = sqsClientConfig,
    sqsConfig = sqsConfig,
    snsClientConfig = snsClientConfig,
    snsConfig = snsConfig,
    metricsConfig = metricsConfig
  )
}

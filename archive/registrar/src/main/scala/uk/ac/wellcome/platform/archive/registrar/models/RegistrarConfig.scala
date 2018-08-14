package uk.ac.wellcome.platform.archive.registrar.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

case class RegistrarConfig(s3ClientConfig: S3ClientConfig,
                           cloudwatchClientConfig: CloudwatchClientConfig,
                           sqsClientConfig: SQSClientConfig,
                           sqsConfig: SQSConfig,
                           snsClientConfig: SnsClientConfig,
                           snsConfig: SNSConfig,
                           hybridStoreConfig: HybridStoreConfig,
                           metricsConfig: MetricsConfig)


case class HybridStoreConfig(
                              dynamoClientConfig: DynamoClientConfig,
                              s3ClientConfig: S3ClientConfig,
                              dynamoConfig: DynamoConfig,
                              s3Config: S3Config,
                              s3GlobalPrefix: String
                            )

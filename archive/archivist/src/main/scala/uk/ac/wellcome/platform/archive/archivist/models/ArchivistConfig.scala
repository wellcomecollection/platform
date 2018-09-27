package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig

case class ArchivistConfig(
  s3ClientConfig: S3ClientConfig,
  bagUploaderConfig: BagUploaderConfig,
  cloudwatchClientConfig: CloudwatchClientConfig,
  sqsClientConfig: SQSClientConfig,
  sqsConfig: SQSConfig,
  snsClientConfig: SnsClientConfig,
  snsConfig: SNSConfig,
  progressMonitorConfig: ProgressMonitorConfig,
  metricsConfig: MetricsConfig
)

case class UploadConfig(uploadNamespace: String,
                        uploadPrefix: String = "archive")

case class BagItConfig(digestDelimiterRegexp: String = " +",
                       tagManifestFilePattern: String = "tagmanifest-%s.txt",
                       manifestFilePattern: String = "manifest-%s.txt",
                       algorithm: String = "sha256") {

  def tagManifestFileName =
    tagManifestFilePattern.format(algorithm)

  def manifestFileName =
    manifestFilePattern.format(algorithm)

  def digestNames = List(tagManifestFileName, manifestFileName)
}

case class BagUploaderConfig(
  uploadConfig: UploadConfig,
  bagItConfig: BagItConfig = BagItConfig(),
  parallelism: Int
)

sealed trait StorageType
case object DigitisedStorageType {
  override def toString = "digitised"
}
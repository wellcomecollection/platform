package uk.ac.wellcome.models.aws

case class AWSConfig(
  region: String,
  accessKey: Option[String],
  secretKey: Option[String]
)

object AWSConfig {
  def apply(region: String, accessKey: String, secretKey: String): AWSConfig =
    AWSConfig(
      region = region,
      accessKey = Some(accessKey),
      secretKey = Some(secretKey)
    )
}

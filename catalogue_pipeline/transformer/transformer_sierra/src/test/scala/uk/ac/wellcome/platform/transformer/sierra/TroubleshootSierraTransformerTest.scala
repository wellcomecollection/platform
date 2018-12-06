package uk.ac.wellcome.platform.transformer.sierra

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.platform.transformer.sierra.transformers.SierraTransformableTestBase
import uk.ac.wellcome.storage.vhs.HybridRecord

class TroubleshootSierraTransformerTest
    extends FunSpec
    with Matchers
    with SierraTransformableTestBase {

  ignore("transforms this message received from SQS") {
    val queueJsonString =
      """
        |{
        |  "Type" : "Notification",
        |  "MessageId" : "5c940368-1de4-598d-9fdc-57176d33c537",
        |  "TopicArn" : "arn:aws:sns:eu-west-1:760097843905:catalogue_sierra_reindex_topic",
        |  "Subject" : "BulkSNSSender",
        |  "Message" : "{\"location\":{\"namespace\":\"wellcomecollection-vhs-sourcedata-sierra\",\"key\":\"27/3076072/7dd114318a87edaf9aea0608682baa531eb91e67783064aca14e8defd9d06b02\"},\"id\":\"3076072\",\"version\":4}",
        |  "Timestamp" : "2018-12-03T17:57:43.289Z",
        |  "SignatureVersion" : "1",
        |  "Signature" : "a4ARVmWuTk6UJkGkUTfWOn24Yqkp7BYCS/TAFCFKg963F/pqRwHLgu26Yl221nLubrqs78CVvizvzQG8BHWX2y8+U+Yn5bsVQHKjkydz7RH/PyRk9c/YVQavZRZ5hm/Elr6UkyLSeK/2q9tCccc6nWkNtSnPAeRRIOFUkAZbwDT1sjBUDat/Af53KGcSfxCREuXxXJk0Ww4jmM6Z0Rwh+MWWSS6wnolY/oguum1NNVDnaM2vdhDCrp34c1MxkUoPtQdQ7F6GIIU3bVdO5NY6BQihmOQBqKdKlQ0WG0zGGo/Nw/32rmLmnxCI0VetCO1u9Kdzv26djhcyaaxKiwi2bg==",
        |  "SigningCertURL" : "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-ac565b8b1a6c5d002d285f9598aa1d9b.pem",
        |  "UnsubscribeURL" : "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:760097843905:catalogue_sierra_reindex_topic:ca9a6ac3-9d30-4b0f-8fdc-c2769399eb7d"
        |}
      """.stripMargin

    val notification: NotificationMessage =
      fromJson[NotificationMessage](queueJsonString).get
    val hybridRecord = fromJson[HybridRecord](notification.body).get

    val s3client =
      AmazonS3ClientBuilder.standard.withRegion("eu-west-1").build()

    val jsonString =
      scala.io.Source
        .fromInputStream(
          s3client
            .getObject(
              hybridRecord.location.namespace,
              hybridRecord.location.key)
            .getObjectContent
        )
        .mkString

    println(s"VHS content = <<$jsonString>>")

    val transformable = fromJson[SierraTransformable](jsonString).get

    transformToWork(transformable)
  }

  val transformer = new SierraTransformableTransformer
}

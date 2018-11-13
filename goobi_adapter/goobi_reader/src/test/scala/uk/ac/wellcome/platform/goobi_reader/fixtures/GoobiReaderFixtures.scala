package uk.ac.wellcome.platform.goobi_reader.fixtures

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

trait GoobiReaderFixtures {

  private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      .withZone(ZoneOffset.UTC)

  def anS3Notification(sourceKey: String,
                       bucketName: String,
                       eventTime: Instant): String =
    s"""{
        | "Records": [
        |     {
        |         "eventVersion": "2.0",
        |         "eventSource": "aws:s3",
        |         "awsRegion": "eu-west-1",
        |         "eventTime": "${dateTimeFormatter.format(eventTime)}",
        |         "eventName": "ObjectCreated:Put",
        |         "userIdentity": {
        |             "principalId": "AWS:AIDAJCQOG2NMLPWL7OVGQ"
        |         },
        |         "requestParameters": {
        |             "sourceIPAddress": "195.143.129.132"
        |         },
        |         "responseElements": {
        |             "x-amz-request-id": "8AA72F36945DF84E",
        |             "x-amz-id-2": "0sF4Z82rcjaJ4WIKZ7OGEFgSNUZcSqH7J459r4KYBsZ6OyVhgpcbGNeM+wI6llctdLviN/8tgMo="
        |         },
        |         "s3": {
        |             "s3SchemaVersion": "1.0",
        |             "configurationId": "testevent",
        |             "bucket": {
        |                 "name": "$bucketName",
        |                 "ownerIdentity": {
        |                     "principalId": "A2BMUDSS9CMZ3O"
        |                 },
        |                 "arn": "arn:aws:s3:::$bucketName"
        |                 },
        |             "object": {
        |                 "key": "$sourceKey",
        |                 "size": 7193624,
        |                 "eTag": "ce96ad12a0e92e97f9c89948967c62e2",
        |                 "sequencer": "005AFEDC82454E713A"
        |             }
        |         }
        |     }
        | ]
        |}""".stripMargin
}

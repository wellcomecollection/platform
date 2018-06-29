package uk.ac.wellcome.platform.reindex_worker.fixtures

import org.scalatest.Assertion
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.platform.reindex_worker.services.{ReindexRecordReaderService, ReindexService}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures.TestWith

trait ReindexServiceFixture extends LocalDynamoDbVersioned with SNS {
  def withReindexService(table: Table, topic: Topic)(
    testWith: TestWith[ReindexService, Assertion]) = {
    val readerService = new ReindexRecordReaderService(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(
        table = table.name,
        index = table.index
      )
    )

    val reindexService = new ReindexService(
      readerService = readerService,
      snsWriter = new SNSWriter(
        snsClient = snsClient,
        snsConfig = SNSConfig(
          topicArn = topic.arn
        )
      )
    )

    testWith(reindexService)
  }
}

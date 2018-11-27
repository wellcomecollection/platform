package uk.ac.wellcome.platform.transformer.miro.fixtures

import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.services.MiroVHSRecordReceiver
import uk.ac.wellcome.platform.transformer.miro.source.MiroTransformableData
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait MiroVHSRecordReceiverFixture extends Messaging with SNS {
  def withMiroVHSRecordReceiver[R](
    topic: Topic,
    bucket: Bucket,
    snsClient: AmazonSNS = snsClient
  )(testWith: TestWith[MiroVHSRecordReceiver, R])(
    implicit objectStore: ObjectStore[MiroTransformableData]): R =
    withMessageWriter[TransformedBaseWork, R](
      bucket,
      topic,
      writerSnsClient = snsClient) { messageWriter =>
      val recordReceiver = new MiroVHSRecordReceiver(
        objectStore = objectStore,
        messageWriter = messageWriter
      )

      testWith(recordReceiver)
    }
}

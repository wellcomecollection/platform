package uk.ac.wellcome.platform.archive.common.progress
import java.util.UUID

import org.scalatest.{Assertion, Inside}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressEvent, ProgressUpdate}

import scala.util.Try

trait ProgressUpdateAssertions extends SNS with Inside {
  def assertTopicReceivesProgressUpdate(
    requestId: UUID,
    progressTopic: SNS.Topic,
    status: Progress.Status)(assert: List[ProgressEvent] => Assertion) = {
    val messages = listMessagesReceivedFromSNS(progressTopic)
    val progressUpdates = messages.map { messageinfo =>
      fromJson[ProgressUpdate](messageinfo.message).get
    }
    progressUpdates.size should be > 0

    progressUpdates
      .map { progressUpdate =>
        Try(inside(progressUpdate) {
          case ProgressUpdate(id, events, actualStatus) =>
            id shouldBe requestId
            actualStatus shouldBe status

            assert(events)

        })

      }
      .filter(_.isSuccess) should have size 1
  }
}

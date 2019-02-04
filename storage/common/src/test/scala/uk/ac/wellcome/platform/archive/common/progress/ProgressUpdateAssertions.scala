package uk.ac.wellcome.platform.archive.common.progress
import java.util.UUID

import grizzled.slf4j.Logging
import org.scalatest.{Assertion, Inside}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.models.bagit.BagId
import uk.ac.wellcome.platform.archive.common.progress.models._

import scala.util.Try

trait ProgressUpdateAssertions extends SNS with Inside with Logging {
  def assertTopicReceivesProgressStatusUpdate[R](requestId: UUID,
                                                 progressTopic: SNS.Topic,
                                                 status: Progress.Status,
                                                 expectedBag: Option[BagId] =
                                                   None)(
    assert: Seq[ProgressEvent] => R): Assertion = {
    val messages = listMessagesReceivedFromSNS(progressTopic)
    val progressUpdates = messages.map { messageinfo =>
      fromJson[ProgressUpdate](messageinfo.message).get
    }.distinct
    progressUpdates.size should be > 0

    val (success, failure) = progressUpdates
      .map { progressUpdate =>
        debug(s"Received ProgressUpdate: $progressUpdate")
        Try(inside(progressUpdate) {
          case ProgressStatusUpdate(id, actualStatus, maybeBag, events) =>
            id shouldBe requestId
            actualStatus shouldBe status
            maybeBag shouldBe expectedBag
            assert(events)
        })
      }
      .partition(_.isSuccess)

    success should have size 1
  }

  def assertTopicReceivesProgressEventUpdate(requestId: UUID,
                                             progressTopic: SNS.Topic)(
    assert: Seq[ProgressEvent] => Assertion): Assertion = {
    val messages = listMessagesReceivedFromSNS(progressTopic)
    val progressUpdates = messages.map { messageinfo =>
      fromJson[ProgressUpdate](messageinfo.message).get
    }
    progressUpdates.size should be > 0

    val (success, failure) = progressUpdates
      .map { progressUpdate =>
        debug(s"Received ProgressUpdate: $progressUpdate")
        Try(inside(progressUpdate) {
          case ProgressEventUpdate(id, events) =>
            id shouldBe requestId

            assert(events)
        })
      }
      .partition(_.isSuccess)

    success.distinct should have size 1
  }
}

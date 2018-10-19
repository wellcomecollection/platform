package uk.ac.wellcome.platform.archive.common.progress
import java.util.UUID

import grizzled.slf4j.Logging
import org.scalatest.{Assertion, Inside}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.platform.archive.common.progress.models.progress._

import scala.util.Try

trait ProgressUpdateAssertions extends SNS with Inside with Logging {
  def assertTopicReceivesProgressStatusUpdate(
    requestId: UUID,
    progressTopic: SNS.Topic,
    status: Progress.Status)(assert: List[ProgressEvent] => Assertion) = {
    val messages = listMessagesReceivedFromSNS(progressTopic)
    val progressUpdates = messages.map { messageinfo =>
      fromJson[ProgressUpdate](messageinfo.message).get
    }
    progressUpdates.size should be > 0

    val (success, failure) = progressUpdates
      .map { progressUpdate =>
        debug(s"Received ProgressUpdate: $progressUpdate")
        Try(inside(progressUpdate) {
          case ProgressStatusUpdate(id, actualStatus, _) =>
            id shouldBe requestId
            actualStatus shouldBe status
        })
      }
      .partition(_.isSuccess)
    success should have size 1
  }

  def assertTopicReceivesProgressEventUpdate(
    requestId: UUID,
    progressTopic: SNS.Topic)(assert: List[ProgressEvent] => Assertion) = {
    val messages = listMessagesReceivedFromSNS(progressTopic)
    val progressUpdates = messages.map { messageinfo =>
      fromJson[ProgressUpdate](messageinfo.message).get
    }
    progressUpdates.size should be > 0

    progressUpdates.map { progressUpdate =>
      debug(s"Received ProgressUpdate: $progressUpdate")
      Try(inside(progressUpdate) {
        case ProgressEventUpdate(id, _) =>
          id shouldBe requestId
      })
    }
  }

  def assertTopicReceivesProgressResourceUpdate(
                                              requestId: UUID,
                                              expectedResource : Resource,
                                              progressTopic: SNS.Topic)(assert: List[ProgressEvent] => Assertion) = {
    val messages = listMessagesReceivedFromSNS(progressTopic)
    val progressUpdates = messages.map { messageinfo =>
      fromJson[ProgressUpdate](messageinfo.message).get
    }
    progressUpdates.size should be > 0

    progressUpdates.map { progressUpdate =>
      debug(s"Received ProgressUpdate: $progressUpdate")
      Try(inside(progressUpdate) {
        case ProgressResourceUpdate(id, resource, _) =>
          id shouldBe requestId
          resource shouldBe expectedResource
      })
    }
  }
}

package uk.ac.wellcome.messaging.message

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.test.utils.ExtendedPatience

class MessageSenderTest
  extends FunSpec
    with Matchers
    with Messaging
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with S3
    with MetricsSenderFixture {


  describe("with S3TypeMessageSender") {
    it("retrieves messages") {

    }
  }

  describe("with TypeMessageSender") {
    it("retrieves messages") {

    }
  }
}

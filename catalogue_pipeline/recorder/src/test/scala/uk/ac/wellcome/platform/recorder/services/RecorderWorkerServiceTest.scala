package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.storage.VersionedHybridStore
import uk.ac.wellcome.test.utils.ExtendedPatience

class RecorderWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with ExtendedPatience {

  it("records an UnidentifiedWork which has never been seen before") {
    ...
  }

  it("records a newer version of an UnidentifiedWork than previously stored") {
    ...
  }

  it("ignores an older version of an UnidentifiedWork than previously stored") {
    ...
  }
}

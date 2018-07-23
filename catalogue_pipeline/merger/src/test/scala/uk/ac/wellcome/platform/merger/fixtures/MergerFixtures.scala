package uk.ac.wellcome.platform.merger.fixtures

import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.merger.services.Merger
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.test.fixtures.{Akka, Fixture, fixture}

trait MergerFixtures
  extends SQS
  with Akka
  with MetricsSenderFixture
  with LocalVersionedHybridStore
  with SNS
  with Messaging {

  def withMerger[R]: Fixture[Merger, R] = fixture[Merger, R](
    create = {
      new Merger()
    }
  )
}

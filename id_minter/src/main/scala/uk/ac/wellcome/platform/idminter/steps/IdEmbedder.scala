package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work

import scala.concurrent.Future

class IdEmbedder @Inject()(metricsSender: MetricsSender,
                           identifierGenerator: IdentifierGenerator)
    extends Logging {
  def embedId(work: Work): Future[Work] = {
    metricsSender.timeAndCount(
      "generate-id",
      () =>
        Future {
          val canonicalId = identifierGenerator
            .retrieveOrGenerateCanonicalId(
              work.identifiers,
              ontologyType = work.ontologyType
            )
            .get
          work.copy(canonicalId = Some(canonicalId))
      }
    )
  }
}

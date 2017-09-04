package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.Logging
import monocle.function.all._
import monocle.macros.GenLens
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{Identifiable, Work}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class IdEmbedder @Inject()(metricsSender: MetricsSender,
                           identifierGenerator: IdentifierGenerator)
    extends Logging {

  def embedId(work: Work): Future[Work] = {
    metricsSender.timeAndCount(
      "generate-id",
      () =>
        Future {
          val canonicalId = callIdGenerator(work)
          val newWork = work.copy(canonicalId = Some(canonicalId))
          (GenLens[Work](_.items) composeTraversal each).modify { item =>
            val canonicalId: String = callIdGenerator(item)
            item.copy(canonicalId = Some(canonicalId))
          }(newWork)
      }
    )
  }

  private def callIdGenerator(identifiable: Identifiable): String = {
    val canonicalId = identifierGenerator
      .retrieveOrGenerateCanonicalId(
        identifiable.identifiers,
        ontologyType = identifiable.ontologyType
      )
      .get
    canonicalId
  }
}

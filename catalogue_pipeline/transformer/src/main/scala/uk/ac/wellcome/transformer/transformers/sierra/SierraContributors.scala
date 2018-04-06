package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.Contributor
import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraContributors extends SierraCreators {
  def getContributors(sierraBibData: SierraBibData): List[Contributor] =
    getCreators(sierraBibData).map(Contributor(_, List()))
}

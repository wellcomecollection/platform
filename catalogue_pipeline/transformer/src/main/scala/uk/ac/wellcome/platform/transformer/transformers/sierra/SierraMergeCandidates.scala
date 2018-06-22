package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.MergeCandidate
import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraMergeCandidates {
  def getMergeCandidates(bibData: SierraBibData): List[MergeCandidate] = {
    List()
  }
}

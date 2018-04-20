package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraContributors extends MarcUtils {

  /* Populate wwork:contributors. Rules:
   *
   * For bib records with MARC tag 100:
   *  - Use subfield $a as "label"
   *
   */
  def getContributors(
    bibData: SierraBibData): List[Contributor[MaybeDisplayable[AbstractAgent]]] = {
    List()
  }
}

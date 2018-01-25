package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraLettering {

  def getLettering(bibData: SierraBibData): Option[String] = {
    Some("foo")
  }
}

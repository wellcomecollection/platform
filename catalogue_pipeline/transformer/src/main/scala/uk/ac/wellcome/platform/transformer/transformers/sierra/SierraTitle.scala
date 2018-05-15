package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraTitle {

  // Populate wwork:title.  The rules are as follows:
  //
  //    For all bibliographic records use Sierra "title".
  //
  // Note: Sierra populates this field from MARC field 245 subfields $a and $b.
  // http://www.loc.gov/marc/bibliographic/bd245.html
  def getTitle(bibData: SierraBibData): Option[String] =
    bibData.title
}

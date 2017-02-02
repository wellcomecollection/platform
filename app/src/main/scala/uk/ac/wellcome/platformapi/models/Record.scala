package uk.ac.wellcome.platform.api.models

import com.sksamuel.elastic4s.searches.RichSearchHit


case class Record(
  refno: String,
  title: String,
  material_type: String
//  date: String,
//  acquisition: String
)
case object Record {
  def apply(esResult: RichSearchHit): Record = {
    val data = esResult.sourceAsMap

    Record(
      refno=data("AltRefNo").toString,
      title=data("Title").toString,
      material_type=data("Material").toString

      //date=data.getOrElse("Date", "Unknown").toString,
      //acquisition=data.getOrElse("Acquisition", "Unknown").toString
    )
  }
}

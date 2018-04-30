package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{Concept, Genre, Period, Place}
import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraGenres extends MarcUtils {

  def getGenres(bibData: SierraBibData): List[Genre] = {
    getGenresForMarcTag(bibData, "655")
  }

  private def getGenresForMarcTag(bibData: SierraBibData, marcTag: String) = {
    val subfieldsList =
      getMatchingSubfields(bibData, marcTag, List("a", "v", "x", "y", "z"))
    subfieldsList.map(subfields => {
      val (subfieldsA, rest) = subfields.partition(_.tag == "a")
      val orderedSubfields = subfieldsA ++ rest
      val label = orderedSubfields.map(_.content).mkString(" - ")
      val concepts = orderedSubfields.map(subfield =>
        subfield.tag match {
          case "y" => Period(label = subfield.content)
          case "z" => Place(label = subfield.content)
          case _ => Concept(label = subfield.content)
      })
      Genre(label, concepts)
    })
  }
}

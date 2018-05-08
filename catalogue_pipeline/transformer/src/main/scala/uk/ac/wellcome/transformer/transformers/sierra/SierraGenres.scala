package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraGenres extends MarcUtils {

  def getGenres(bibData: SierraBibData): List[Genre[MaybeDisplayable[AbstractConcept]]] = {
    getGenresForMarcTag(bibData, "655")
  }

  // Populate wwork:genres
  //
  // Use MARC field "655"
  //
  // Each Genre type is populated with label and concepts
  //
  //   - Genre.label is concatenated subfields a,v,x,y,z in order separated by a hyphen ' - '.
  //   - Genre.concepts is a List populated with subfield 'a', then Concepts from subfields v,x,y,z in order with types:
  //       * v => Concept
  //       * x => Concept
  //       * y => Period
  //       * z => Place
  //
  private def getGenresForMarcTag(bibData: SierraBibData, marcTag: String) = {
    val subfieldsList =
      getMatchingSubfields(bibData, marcTag, List("a", "v", "x", "y", "z"))
    subfieldsList.map(subfields => {
      val (subfieldsA, rest) = subfields.partition(_.tag == "a")
      val orderedSubfields = subfieldsA ++ rest
      val label = orderedSubfields.map(_.content).mkString(" - ")
      val concepts = orderedSubfields.map(subfield =>
        subfield.tag match {
          case "y" => MaybeDisplayable[Period](label = subfield.content)
          case "z" => MaybeDisplayable[Place](label = subfield.content)
          case _ => MaybeDisplayable[Concept](label = subfield.content)
      })
      Genre[MaybeDisplayable[AbstractConcept]](
        label = label,
        concepts = concepts
      )
    })
  }
}

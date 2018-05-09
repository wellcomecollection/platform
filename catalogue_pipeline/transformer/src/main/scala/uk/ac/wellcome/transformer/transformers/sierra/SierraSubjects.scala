package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Concept,
  MaybeDisplayable,
  Period,
  Place,
  Subject,
  Unidentifiable
}
import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraSubjects extends MarcUtils {

  def getSubjects(bibData: SierraBibData)
    : List[Subject[MaybeDisplayable[AbstractConcept]]] = {
    getSubjectsForMarcTag(bibData, "650") ++
      getSubjectsForMarcTag(bibData, "648") ++
      getSubjectsForMarcTag(bibData, "651")
  }

  // Populate wwork:subjects
  //
  // Use MARC fields "650", "648", "651"
  //
  // Each Subject type is populated with label and concepts
  //
  //   - Subject.label is concatenated subfields a,v,x,y,z in order separated by a hyphen ' - '.
  //   - Subject.concepts is a List populated with a primary Concept with label from subfield 'a', and
  //     type:
  //       650 => Concept
  //       648 => Period
  //       651 => Place
  //     for subfields v,x,y,z Subject.concepts are populated in order with Concept.label and type:
  //       * v => Concept
  //       * x => Concept
  //       * y => Period
  //       * z => Place
  //
  private def getSubjectsForMarcTag(bibData: SierraBibData, marcTag: String) = {
    val subfieldsList = getMatchingSubfields(
      bibData,
      marcTag = marcTag,
      marcSubfieldTags = List("a", "v", "x", "y", "z"))
    subfieldsList.map(subfields => {
      val (subfieldsA, rest) = subfields.partition(_.tag == "a")
      val orderedSubfields = subfieldsA ++ rest
      val subjectLabel = orderedSubfields.map(_.content).mkString(" - ")
      val concepts = orderedSubfields.map(subfield =>
        subfield.tag match {
          case "a" => Unidentifiable(primaryConcept(marcTag, subfield.content))
          case "y" => Unidentifiable(Period(label = subfield.content))
          case "z" => Unidentifiable(Place(label = subfield.content))
          case _ => Unidentifiable(Concept(label = subfield.content))
      })
      Subject[MaybeDisplayable[AbstractConcept]](
        label = subjectLabel,
        concepts = concepts
      )
    })
  }

  private def primaryConcept(marcTag: String, label: String) = {
    marcTag match {
      case "650" => Concept(label)
      case "648" => Period(label)
      case "651" => Place(label)
    }
  }
}

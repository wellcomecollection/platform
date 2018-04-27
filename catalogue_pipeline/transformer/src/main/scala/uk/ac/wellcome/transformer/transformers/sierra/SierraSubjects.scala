package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraBibData
import uk.ac.wellcome.work_model
import uk.ac.wellcome.work_model.{Concept, Period, Place, Subject}

trait SierraSubjects extends MarcUtils {

  def getSubjects(bibData: SierraBibData): List[Subject] = {
    getSubjectsForMarcTag(bibData, "650") ++
      getSubjectsForMarcTag(bibData, "648") ++
      getSubjectsForMarcTag(bibData, "651")
  }

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
          case "a" => primaryConcept(marcTag, subfield.content)
          case "y" => Period(label = subfield.content)
          case "z" => Place(label = subfield.content)
          case _ => Concept(label = subfield.content)
      })
      work_model.Subject(subjectLabel, concepts)
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

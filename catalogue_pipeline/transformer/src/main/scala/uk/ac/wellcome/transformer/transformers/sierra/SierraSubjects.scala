package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.{Concept, Subject}
import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraSubjects extends MarcUtils {

  def getSubjects(bibData: SierraBibData) : List[Subject] = {
    val subfieldsList = getMatchingSubfields(bibData, marcTag = "650", marcSubfieldTags = List("a", "v", "x"))
    subfieldsList.map( subfields => {
      val (subfieldsA, rest) = subfields.partition(_.tag == "a")
      val orderedSubfields = subfieldsA ++ rest.sortBy(_.tag)
      val subjectLabel = orderedSubfields.map( _.content).mkString(" - ")
      val concepts = orderedSubfields.map( subfield => Concept(label = subfield.content))
      Subject(subjectLabel, concepts)
    } )
  }
}

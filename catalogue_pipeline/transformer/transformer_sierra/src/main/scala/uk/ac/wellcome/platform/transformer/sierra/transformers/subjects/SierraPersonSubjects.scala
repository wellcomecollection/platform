package uk.ac.wellcome.platform.transformer.sierra.transformers.subjects

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.transformers.{
  MarcUtils,
  SierraAgents
}

trait SierraPersonSubjects extends MarcUtils with SierraAgents {

  // Populate wwork:subject
  //
  // Use MARC field "600" where the second indicator is not 7.
  //
  // The concepts come from:
  //
  //    - The person
  //    - The contents of subfields $t and $x (title and general subdivision),
  //      both as Concepts
  //
  // The label is constructed concatenating subfields $a, $b, $c, $d, $e,
  // where $d and $e represent the person's dates and roles respectively.
  //
  // The person can be identified if there is an identifier in subfield $0 and the second indicator is "0".
  // If second indicator is anything other than 0, we don't expose the identifier for now.
  //
  def getSubjectsWithPerson(bibData: SierraBibData)
    : List[MaybeDisplayable[Subject[MaybeDisplayable[AbstractRootConcept]]]] = {
    val marcVarFields = getMatchingVarFields(bibData, marcTag = "600")

    // Second indicator 7 means that the subject authority is something other
    // than library of congress or mesh. Some MARC records have duplicated subjects
    // when the same subject has more than one authority (for example mesh and FAST),
    // which causes duplicated subjects to appear in the API.
    //
    // So let's filter anything that is from another authority for now.
    marcVarFields
      .filterNot { _.indicator2.contains("7") }
      .flatMap { varField: VarField =>
        val subfields = varField.subfields
        val maybePerson = getPerson(subfields)
        val generalSubdivisions =
          varField.subfields
            .collect {
              case MarcSubfield("t", content) => content
              case MarcSubfield("x", content) => content
            }

        maybePerson.map { person =>
          val label = getPersonSubjectLabel(
            person = person,
            roles = getRoles(subfields),
            dates = getDates(subfields),
            generalSubdivisions = generalSubdivisions
          )

          val subject = Subject(
            label = label,
            concepts = getConcepts(person, generalSubdivisions)
          )

          varField.indicator2 match {
            case Some("0") => identify(varField.subfields, subject, "Subject")
            case _         => Unidentifiable(subject)
          }
        }
      }
  }

  private def getPersonSubjectLabel(person: Person,
                                    roles: List[String],
                                    dates: Option[String],
                                    generalSubdivisions: List[String]): String =
    (List(person.label) ++ person.numeration ++ person.prefix ++ dates ++ roles ++ generalSubdivisions)
      .mkString(" ")

  private def getConcepts(person: Person, generalSubdivisions: List[String])
    : List[MaybeDisplayable[AbstractRootConcept]] = {
    val personConcept = Unidentifiable(person)

    val generalSubdivisionConcepts =
      generalSubdivisions
        .map { label =>
          Unidentifiable(Concept(label))
        }

    personConcept +: generalSubdivisionConcepts
  }

  private def getRoles(secondarySubfields: List[MarcSubfield]) =
    secondarySubfields.collect { case MarcSubfield("e", role) => role }
  private def getDates(secondarySubfields: List[MarcSubfield]) =
    secondarySubfields.find(_.tag == "d").map(_.content)
}

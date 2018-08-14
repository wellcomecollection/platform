package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData, VarField}
import uk.ac.wellcome.platform.transformer.transformers.ShouldNotTransformException

trait SierraSubjects extends MarcUtils with SierraConcepts {

  // Populate wwork:subject
  //
  // Use MARC field "650", "648" and "651" where the second indicator is not 7.
  //
  // Within these MARC tags, we have:
  //
  //    - a primary concept (subfield $a); and
  //    - subdivisions (subfields $v, $x, $y and $z)
  //
  // The primary concept can be identified, and the subdivisions serve
  // to add extra context.
  //
  // We construct the Subject as follows:
  //
  //    - label is the concatenation of $a, $v, $x, $y and $z in order,
  //      separated by a hyphen ' - '.
  //    - concepts is a List[Concept] populated in order of the subfields:
  //
  //        * $a => {Concept, Period, Place}
  //          Optionally with an identifier.  We look in subfield $0 for the
  //          identifier value, then second indicator for the authority.
  //          These are decided as follows:
  //
  //            - 650 => Concept
  //            - 648 => Period
  //            - 651 => Place
  //
  //        * $v => Concept
  //        * $x => Concept
  //        * $y => Period
  //        * $z => Place
  //
  //      Note that only concepts from subfield $a are identified; everything
  //      else is unidentified.
  //
  def getSubjects(bibData: SierraBibData)
    : List[Subject[MaybeDisplayable[AbstractConcept]]] = {
    getSubjectswithAbstractConcepts(bibData, "650") ++
      getSubjectswithAbstractConcepts(bibData, "648") ++
      getSubjectswithAbstractConcepts(bibData, "651") ++
      getSubjectsWithAbstractAgents(bibData, "600")++
      getSubjectsWithAbstractAgents(bibData, "610")
  }

  private def getSubjectswithAbstractConcepts(bibData: SierraBibData, marcTag: String) = {
    val marcVarFields = getMatchingVarFields(bibData, marcTag = marcTag)

    // Second indicator 7 means that the subject authority is something other
    // than library of congress or mesh. Some MARC records have duplicated subjects
    // when the same subject has more than one authority (for example mesh and FAST),
    // which causes duplicated subjects to appear in the API.
    // So let's filter anything that is from another authority for now.
    marcVarFields.filterNot(_.indicator2.contains("7")).map { varField =>
      val subfields = filterSubfields(varField, List("a", "v", "x", "y", "z"))
      val (primarySubfields, subdivisionSubfields) = subfields.partition {
        _.tag == "a"
      }

      val label = getLabel(primarySubfields, subdivisionSubfields)
      val concepts: List[MaybeDisplayable[AbstractConcept]] = getAbstractConceptPrimaryConcept(
        primarySubfields,
        varField = varField) ++ getSubdivisions(subdivisionSubfields)

      Subject[MaybeDisplayable[AbstractConcept]](
        label = label,
        concepts = concepts
      )
    }
  }

  private def getSubjectsWithAbstractAgents(bibData: SierraBibData, marcTag: String) = {
    val marcVarFields = getMatchingVarFields(bibData, marcTag = marcTag)

    // Second indicator 7 means that the subject authority is something other
    // than library of congress or mesh. Some MARC records have duplicated subjects
    // when the same subject has more than one authority (for example mesh and FAST),
    // which causes duplicated subjects to appear in the API.
    // So let's filter anything that is from another authority for now.
    marcVarFields.filterNot(_.indicator2.contains("7")).map { varField =>
      val subfields = varField.subfields
      val (primarySubfields, secondarySubfields) = subfields.partition {
        _.tag == "a"
      }
      val person = getPerson(subfields)
      val label = getAbstractAgentLabel(subfields)
      val primaryConcept = getAbstractAgentPrimaryConcept(
        person,
        primarySubfields,
        secondarySubfields,
        varField = varField)

      Subject(
        label = label,
        concepts = primaryConcept
      )
    }
  }

  private def getPerson(subfields: List[MarcSubfield]) = {
    val (primarySubfields, secondarySubfields) = subfields.partition {
      _.tag == "a"
    }
    val prefix: Option[String] = getPrefix(secondarySubfields)
    val numeration: Option[String] = getNumeration(secondarySubfields)
    val dates: Option[String] = getDates(secondarySubfields)
    primarySubfields match {
      case List(MarcSubfield(_, name)) =>Person(label = name, prefix = prefix, numeration = numeration, dates = dates)
      case _ => throw new ShouldNotTransformException("jhgdfjh")
    }

  }

  private def getAbstractAgentLabel(subfields: List[MarcSubfield]) = {
    val name = subfields.find(_.tag == "a").map(_.content).toList
    val prefix = getPrefix(subfields).toList
    val numeration = getNumeration(subfields).toList
    val role = getRole(subfields).toList
    (List((prefix ++ name ++ numeration).mkString(" ")) ++ role).mkString(", ")
  }

  private def filterSubfields(varField: VarField, subfields: List[String]) = {
    varField.subfields.filter { subfield =>
      subfields.contains(subfield.tag)
    }
  }

  private def getAbstractConceptPrimaryConcept(
    primarySubfields: List[MarcSubfield],
    varField: VarField): List[MaybeDisplayable[AbstractConcept]] = {
    primarySubfields.map { subfield =>
      varField.marcTag.get match {
        case "650" =>
          identifyPrimaryConcept(
            concept = Concept(label = subfield.content),
            varField = varField
          )
        case "648" =>
          identifyPrimaryConcept(
            concept = Period(label = subfield.content),
            varField = varField
          )
        case "651" =>
          identifyPrimaryConcept(
            concept = Place(label = subfield.content),
            varField = varField
          )
      }

    }
  }

  private def getAbstractAgentPrimaryConcept(
                                            person:Person,
    primarySubfields: List[MarcSubfield],
    secondarySubfields: List[MarcSubfield],
    varField: VarField): List[MaybeDisplayable[AbstractConcept]] = {
    primarySubfields.map { subfield =>
      varField.marcTag.get match {
        case "600" =>
          identifyPrimaryConcept(
            concept = person,
            varField = varField
          )
        case "610" =>
          identifyPrimaryConcept(
            concept = Organisation(label = subfield.content),
            varField = varField
          )
      }
    }
  }

  private def getPrefix(secondarySubfields: List[MarcSubfield]) = {
    val prefixes = secondarySubfields.collect {
      case MarcSubfield("c", content) => content
    }
    val prefixString =
      if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))
    prefixString
  }

  private def getNumeration(secondarySubfields: List[MarcSubfield]) = secondarySubfields.find(_.tag == "b").map(_.content)
  private def getRole(secondarySubfields: List[MarcSubfield]) = secondarySubfields.find(_.tag == "e").map(_.content)
  private def getDates(secondarySubfields: List[MarcSubfield]) = secondarySubfields.find(_.tag == "d").map(_.content)
}

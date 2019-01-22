package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

trait SierraProduction {

  // Populate wwork:production.
  //
  // Information about production can come from two fields in MARC: 260 & 264.
  // At Wellcome, 260 is what was used historically -- 264 is what we're moving
  // towards, using RDA rules.
  //
  // It is theoretically possible for a bib record to have both 260 and 264,
  // but it would be a cataloguing error -- we should reject it, and flag it
  // to the librarians.
  //
  def getProduction(bibData: SierraBibData)
    : List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = {
    val maybeMarc260fields = bibData.varFields.filter {
      _.marcTag.contains("260")
    }
    val maybeMarc264fields = bibData.varFields.filter {
      _.marcTag.contains("264")
    }

    (maybeMarc260fields, maybeMarc264fields) match {
      case (Nil, Nil)           => List()
      case (marc260fields, Nil) => getProductionFrom260Fields(marc260fields)
      case (Nil, marc264fields) => getProductionFrom264Fields(marc264fields)
      case (marc260fields, marc264fields) =>
        getProductionFromBothFields(marc260fields, marc264fields)
    }
  }

  // Populate wwork:production from MARC tag 260.
  //
  // The rules are as follows:
  //
  //  - Populate "places" from subfield "a" and type as "Place"
  //  - Populate "agents" from subfield "b" and type as "Agent"
  //  - Populate "dates" from subfield "c" and type as "Period"
  //
  // If any of the following fields are included, we add them to the
  // existing places/agents/dates field, _and_ set the productionFunction
  // to "Manufacture":
  //
  //  - Extra places from subfield "e"
  //  - Extra agents from subfield "f"
  //  - Extra dates from subfield "g"
  //
  // If we don't have any of these fields, we can't tell what the
  // productionFunction is, so we should leave it as "None".
  //
  // Note: Regardless of their order in the MARC, these fields _always_
  // appear after a/b/c.  This is an implementation detail, not described
  // in the transform rules.
  // TODO: Check if this is okay.
  //
  // Note: a, b, c, e, f and g are all repeatable fields in the MARC spec.
  //
  // https://www.loc.gov/marc/bibliographic/bd260.html
  //
  private def getProductionFrom260Fields(varFields: List[VarField]) =
    varFields.map { vf =>
      val label = labelFromSubFields(vf)
      val places = placesFromSubfields(vf, subfieldTag = "a")
      val agents = agentsFromSubfields(vf, subfieldTag = "b")
      val dates = datesFromSubfields(vf, subfieldTag = "c")

      val extraPlaces = placesFromSubfields(vf, subfieldTag = "e")
      val extraAgents = agentsFromSubfields(vf, subfieldTag = "f")
      val extraDates = datesFromSubfields(vf, subfieldTag = "g")

      val productionFunction =
        if (extraPlaces != Nil || extraAgents != Nil || extraDates != Nil) {
          Some(Concept(label = "Manufacture"))
        } else None

      ProductionEvent(
        label = label,
        places = places ++ extraPlaces,
        agents = agents ++ extraAgents,
        dates = dates ++ extraDates,
        function = productionFunction
      )
    }

  // Populate wwork:production from MARC tag 264.
  //
  // The rules are as follows:
  //
  //  - Populate "places" from subfield "a" and type as "Place"
  //  - Populate "agents" from subfield "b" and type as "Agent"
  //  - Populate "dates" from subfield "c" and type as "Period"
  //
  // The production function is set based on the second indicator, as defined
  // in the MARC spec.
  //
  //  - 0 = Production
  //  - 1 = Publication
  //  - 2 = Distribution
  //  - 3 = Manufacture
  //
  // The MARC spec specifies another value for the production function:
  //
  //  - 4 = Copyright notice date
  //
  // We'll be putting copyright information in a separate part of the domain
  // model, so we drop any fields with indicator 4 for production events.
  //
  // Note that a, b and c are repeatable fields.
  //
  // https://www.loc.gov/marc/bibliographic/bd264.html
  //
  private def getProductionFrom264Fields(varFields: List[VarField]) =
    varFields
      .filterNot { vf =>
        vf.indicator2.contains("4") || vf.indicator2.contains(" ")
      }
      .map { vf =>
        val label = labelFromSubFields(vf)
        val places = placesFromSubfields(vf, subfieldTag = "a")
        val agents = agentsFromSubfields(vf, subfieldTag = "b")
        val dates = datesFromSubfields(vf, subfieldTag = "c")

        val productionFunctionLabel = vf.indicator2 match {
          case Some("0") => "Production"
          case Some("1") => "Publication"
          case Some("2") => "Distribution"
          case Some("3") => "Manufacture"
          case other =>
            throw SierraTransformerException(
              s"Unrecognised second indicator for production function: [$other]"
            )
        }

        val productionFunction = Some(Concept(label = productionFunctionLabel))

        ProductionEvent(
          label = label,
          places = places,
          agents = agents,
          dates = dates,
          function = productionFunction
        )
      }

  private def marc264OnlyContainsCopyright(
    marc264fields: List[VarField]): Boolean =
    marc264fields match {
      case List(
          VarField(_, Some("264"), _, _, List(MarcSubfield("c", content)))) =>
        content.matches("^©\\d{4}$")
      case _ => false
    }

  /** Populate the production data if both 260 and 264 are present.
    *
    * In general, this is a cataloguing error, but sometimes we can do
    * something more sensible depending on if/how they're duplicated.
    */
  private def getProductionFromBothFields(marc260fields: List[VarField],
                                          marc264fields: List[VarField]) = {

    // We've seen cases where the 264 field only has the following subfields:
    //
    //      [('tag', 'c'), ('content', '©2012')]
    //
    // or similar, and the 260 field is populated.  In that case, we can
    // discard the 264 and just use the 260 fields.
    if (marc264OnlyContainsCopyright(marc264fields)) {
      getProductionFrom260Fields(marc260fields)
    }

    // We've also seen cases where the 260 and 264 field are both present,
    // and they have matching subfields!  We use the 260 field as it's not
    // going to throw an exception about unrecognised second indicator.
    else if (marc260fields.map { _.subfields } ==
               marc264fields.map { _.subfields }) {
      getProductionFrom260Fields(marc260fields)
    }

    // Otherwise this is some sort of cataloguing error.  This is fairly
    // rare, so let it bubble on to a DLQ.
    else {
      throw SierraTransformerException(
        "Record has both 260 and 264 fields; this is a cataloguing error."
      )
    }
  }

  // @@AWLC: I'm joining these with a space because that seems more appropriate
  // given our catalogue, but the MARC spec isn't entirely clear on what to do.
  //
  // The convention used in the current Library website is to use a string.
  // Two examples, both retrieved 22 January 2019:
  //
  // bib 1548327:
  //    MARC        260    [Horsham] :|cCats Protection League,|c[ca.1990?]
  //    Website     [Horsham] : Cats Protection League, [ca.1990?]
  //
  // bib 2847879:
  //    MARC        264  0 [Netherne, Surrey],|c[ca. 1966]
  //    Website     [Netherne, Surrey], [ca. 1966]
  //
  private def labelFromSubFields(vf: VarField): String =
    vf.subfields.map { _.content }.mkString(" ")

  private def placesFromSubfields(vf: VarField,
                                  subfieldTag: String): List[Place] =
    vf.subfields
      .filter { _.tag == subfieldTag }
      .map { sf: MarcSubfield =>
        Place.normalised(label = sf.content)
      }

  private def agentsFromSubfields(
    vf: VarField,
    subfieldTag: String): List[Unidentifiable[Agent]] =
    vf.subfields
      .filter { _.tag == subfieldTag }
      .map { sf: MarcSubfield =>
        Unidentifiable(Agent.normalised(label = sf.content))
      }

  private def datesFromSubfields(vf: VarField,
                                 subfieldTag: String): List[Period] =
    vf.subfields
      .filter { _.tag == subfieldTag }
      .map { sf: MarcSubfield =>
        Period.normalised(label = sf.content)
      }
}

package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{
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
      _.marcTag == Some("260")
    }
    val maybeMarc264fields = bibData.varFields.filter {
      _.marcTag == Some("264")
    }

    (maybeMarc260fields, maybeMarc264fields) match {
      case (Nil, Nil) => List()
      case (marc260fields, Nil) => getProductionFrom260Fields(marc260fields)
      case (Nil, marc264fields) => getProductionFrom264Fields(marc264fields)
      case (_, _) =>
        throw new GracefulFailureException(
          new RuntimeException(
            "Record has both 260 and 264 fields; this is a cataloguing error."
          ))
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
      .filterNot { vf => vf.indicator2 == Some("4") }
      .map { vf =>
        val places = placesFromSubfields(vf, subfieldTag = "a")
        val agents = agentsFromSubfields(vf, subfieldTag = "b")
        val dates = datesFromSubfields(vf, subfieldTag = "c")

        val productionFunctionLabel = vf.indicator2 match {
          case Some("0") => "Production"
          case Some("1") => "Publication"
          case Some("2") => "Distribution"
          case Some("3") => "Manufacture"
          case other =>
            throw GracefulFailureException(new RuntimeException(
              s"Unrecognised second indicator for production function: [$other]"
            ))
        }

        val productionFunction = Some(Concept(label = productionFunctionLabel))

        ProductionEvent(
          places = places,
          agents = agents,
          dates = dates,
          function = productionFunction
        )
      }

  private def placesFromSubfields(vf: VarField,
                                  subfieldTag: String): List[Place] =
    vf.subfields
      .filter { _.tag == subfieldTag }
      .map { sf: MarcSubfield =>
        Place(label = sf.content)
      }

  private def agentsFromSubfields(
    vf: VarField,
    subfieldTag: String): List[Unidentifiable[Agent]] =
    vf.subfields
      .filter { _.tag == subfieldTag }
      .map { sf: MarcSubfield =>
        Unidentifiable(Agent(label = sf.content))
      }

  private def datesFromSubfields(vf: VarField,
                                 subfieldTag: String): List[Period] =
    vf.subfields
      .filter { _.tag == subfieldTag }
      .map { sf: MarcSubfield =>
        Period(label = sf.content)
      }
}

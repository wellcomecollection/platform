package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData, VarField}

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
    val maybeMarc260fields = bibData.varFields.filter { _.marcTag == Some("260") }
    val maybeMarc264fields = bibData.varFields.filter { _.marcTag == Some("264") }

    (maybeMarc260fields, maybeMarc264fields) match {
      case (Nil, Nil) => List()
      case (marc260fields, Nil) => getProductionFrom260Fields(marc260fields)
      case (Nil, _) => List()
      case (_, _) => throw new GracefulFailureException(new RuntimeException(
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
  private def getProductionFrom260Fields(varFields: List[VarField]) =
    varFields.map { vf =>
      val places = placesFromSubfields(vf, subfieldTag = "a")
      val agents = agentsFromSubfields(vf, subfieldTag = "b")

      val dates: List[Period] = vf.subfields
        .filter { _.tag == "c" }
        .map { sf: MarcSubfield => Period(label = sf.content) }

      val extraPlaces = placesFromSubfields(vf, subfieldTag = "e")
      val extraAgents = agentsFromSubfields(vf, subfieldTag = "f")

      val productionFunction = if (extraPlaces != Nil || extraAgents != Nil) {
        Some(Concept(label = "Manufacture"))
      } else None

      ProductionEvent(
        places = places ++ extraPlaces,
        dates = dates,
        agents = agents ++ extraAgents,
        productionFunction = productionFunction
      )
    }

  private def placesFromSubfields(vf: VarField, subfieldTag: String): List[Place] =
    vf.subfields
      .filter { _.tag == subfieldTag}
      .map { sf: MarcSubfield => Place(label = sf.content) }

  private def agentsFromSubfields(vf: VarField, subfieldTag: String): List[Unidentifiable[Agent]] =
    vf.subfields
      .filter { _.tag == subfieldTag}
      .map { sf: MarcSubfield => Agent(label = sf.content) }
      .map { ag: Agent => Unidentifiable(ag) }
}

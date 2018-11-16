package uk.ac.wellcome.display.json

import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.{Encoder, Printer}
import io.circe.syntax._
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.display.models.v1._
import uk.ac.wellcome.display.models.v2._

/** Format JSON objects as suitable for display.
  *
  * This is different from `JsonUtil` for a couple of reasons:
  *
  *   - We enable autoderivation for a smaller number of types.  We should never
  *     see `URL` or `UUID` in our display models, so we don't have encoders here.
  *   - We omit null values, but display empty lists.  For internal apps, we always
  *     render the complete value for disambiguation.
  *
  */
object DisplayJsonUtil extends AutoDerivation {
  private val printer = Printer.noSpaces.copy(
    dropNullValues = true
  )

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults

  def toJson[T](value: T)(implicit encoder: Encoder[T]): String = {
    assert(encoder != null)
    printer.pretty(value.asJson)
  }

  // Circe wants to add a type discriminator, and we don't want it to!  Doing so
  // would expose internal names like "DisplayDigitalLocationV1" in the public JSON.
  // So instead we have to do the slightly less nice thing of encoding all the subclasses
  // here by hand.  Annoying, it is.
  //
  // It is the "recommended: approach in the Circe docs:
  // https://circe.github.io/circe/codecs/adt.html

  implicit val locationV1Encoder: Encoder[DisplayLocationV1] = {
    case digitalLocation: DisplayDigitalLocationV1   => digitalLocation.asJson
    case physicalLocation: DisplayPhysicalLocationV1 => physicalLocation.asJson
  }

  implicit val abstractAgentEncoder: Encoder[DisplayAbstractAgentV2] = {
    case agent: DisplayAgentV2               => agent.asJson
    case person: DisplayPersonV2             => person.asJson
    case organisation: DisplayOrganisationV2 => organisation.asJson
  }

  implicit val abstractConceptEncoder: Encoder[DisplayAbstractConcept] = {
    case concept: DisplayConcept => concept.asJson
    case place: DisplayPlace     => place.asJson
    case period: DisplayPeriod   => period.asJson
  }

  implicit val abstractRootConceptEncoder
    : Encoder[DisplayAbstractRootConcept] = {
    case agent: DisplayAbstractAgentV2   => agent.asJson
    case concept: DisplayAbstractConcept => concept.asJson
  }

  implicit val locationV2Encoder: Encoder[DisplayLocationV2] = {
    case digitalLocation: DisplayDigitalLocationV2   => digitalLocation.asJson
    case physicalLocation: DisplayPhysicalLocationV2 => physicalLocation.asJson
  }

  implicit val displayWorkEncoder: Encoder[DisplayWork] = {
    case work: DisplayWorkV1 => work.asJson
    case work: DisplayWorkV2 => work.asJson
  }
}

package uk.ac.wellcome.models

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.{
  JsonDeserialize,
  JsonSerialize
}
import com.twitter.inject.Logging

class IdentifierSchemeDeserialiser
    extends JsonDeserializer[IdentifierSchemes.IdentifierScheme]
    with Logging {

  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext): IdentifierSchemes.IdentifierScheme = {
    val node: JsonNode = p.getCodec.readTree(p)
    val identifierScheme = node.asText()
    IdentifierSchemes.createIdentifierScheme(identifierScheme)
  }
}

class IdentifierSchemeSerialiser
    extends JsonSerializer[IdentifierSchemes.IdentifierScheme] {
  override def serialize(value: IdentifierSchemes.IdentifierScheme,
                         gen: JsonGenerator,
                         serializers: SerializerProvider): Unit = {
    gen.writeString(value.toString)
  }
}

/** This is the canonical version of our identifier schemes.  This contains
  *  the strings that will be presented to users of the API.
  */
object IdentifierSchemes {
  @JsonDeserialize(using = classOf[IdentifierSchemeDeserialiser])
  @JsonSerialize(using = classOf[IdentifierSchemeSerialiser])
  sealed trait IdentifierScheme
  // Corresponds to the image number in Miro, e.g. V00127563.
  case object miroImageNumber extends IdentifierScheme {
    override def toString: String = "miro-image-number"
  }

  // Corresponds to legacy image IDs from the Miro data.  Eventually these
  // should be tackled properly: either assigned proper identifier schemes
  // and normalised, or removed entirely.  For now, this is a stopgap to
  // get these IDs into the API until we deal with them properly.
  case object miroLibraryReference extends IdentifierScheme {
    override def toString: String = "miro-library-reference"
  }

  // Placeholder until we ingest real Calm records.
  // TODO: Replace this with something more appropriate
  case object calmPlaceholder extends IdentifierScheme {
    override def toString: String = "calm-placeholder"
  }

  case object calmAltRefNo extends IdentifierScheme {
    override def toString: String = "calm-altrefno"
  }

  case object sierraSystemNumber extends IdentifierScheme {
    override def toString: String = "sierra-system-number"
  }

  def createIdentifierScheme(
    identifierScheme: String): IdentifierSchemes.IdentifierScheme = {
    identifierScheme match {
      case s: String if s == IdentifierSchemes.miroImageNumber.toString =>
        IdentifierSchemes.miroImageNumber
      case s: String if s == IdentifierSchemes.sierraSystemNumber.toString =>
        IdentifierSchemes.sierraSystemNumber
      case s: String if s == IdentifierSchemes.calmAltRefNo.toString =>
        IdentifierSchemes.calmAltRefNo
      case s: String if s == IdentifierSchemes.calmPlaceholder.toString =>
        IdentifierSchemes.calmPlaceholder
      case s: String if s == IdentifierSchemes.miroLibraryReference.toString =>
        IdentifierSchemes.miroLibraryReference
      case identifierScheme =>
        val errorMessage = s"$identifierScheme is not a valid identifierScheme"
        error(errorMessage)
        throw new Exception(errorMessage)
    }
  }
}

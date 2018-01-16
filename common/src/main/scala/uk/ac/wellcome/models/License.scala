package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import com.twitter.inject.Logging
import io.circe.{Encoder, Json}

@JsonDeserialize(using = classOf[LicenseDeserialiser])
sealed case class License (licenseType: String,label: String,url: String){
  @JsonProperty("type") val ontologyType: String = "License"
}

class LicenseDeserialiser extends JsonDeserializer[License] with Logging {

  override def deserialize(p: JsonParser,
                           ctxt: DeserializationContext): License = {
    val node: JsonNode = p.getCodec.readTree(p)
    val licenseType = node.get("licenseType").asText
    createLicense(licenseType)
  }

  private def createLicense(licenseType: String): License = {
    licenseType match {
      case s: String if s == License_CCBY.licenseType => License_CCBY
      case s: String if s == License_CCBYNC.licenseType => License_CCBYNC
      case s: String if s == License_CCBYNCND.licenseType => License_CCBYNCND
      case s: String if s == License_CC0.licenseType => License_CC0
      case s: String if s == License_PDM.licenseType => License_PDM
      case licenseType =>
        val errorMessage = s"$licenseType is not a valid licenseType"
        error(errorMessage)
        throw new Exception(errorMessage)
    }
  }
}

object License_CCBY extends License(
  licenseType = "CC-BY",
  label = "Attribution 4.0 International (CC BY 4.0)",
  url = "http://creativecommons.org/licenses/by/4.0/"
)

object License_CCBYNC extends License (
  licenseType = "CC-BY-NC",
  label = "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)",
  url = "https://creativecommons.org/licenses/by-nc/4.0/"
)

object License_CCBYNCND extends License (
  licenseType = "CC-BY-NC-ND",
  label = "Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0)",
   url = "https://creativecommons.org/licenses/by-nc-nd/4.0/"
)

object License_CC0 extends License (
   licenseType = "CC-0",
   label = "CC0 1.0 Universal",
   url = "https://creativecommons.org/publicdomain/zero/1.0/legalcode"
)

object License_PDM extends License (
   licenseType = "PDM",
   label = "Public Domain Mark",
   url = "https://creativecommons.org/share-your-work/public-domain/pdm/"
)

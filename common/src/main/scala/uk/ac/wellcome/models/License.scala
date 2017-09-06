package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

sealed trait License {
  val licenseType: String
  val label: String
  val url: String
  @JsonProperty("type") val ontologyType: String = "License"
}

case object License_CCBY extends License {
  val licenseType = "CC-BY"
  val label = "Attribution 4.0 International (CC BY 4.0)"
  val url = "http://creativecommons.org/licenses/by/4.0/"
}

case object License_CCBYNC extends License {
  val licenseType = "CC-BY-NC"
  val label = "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)"
  val url = "https://creativecommons.org/licenses/by-nc/4.0/"
}

case object License_CCBYNCND extends License {
  val licenseType = "CC-BY-NC-ND"
  val label =
    "Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0)"
  val url = "https://creativecommons.org/licenses/by-nc-nd/4.0/"
}

case object License_CC0 extends License {
  val licenseType = "CC-0"
  val label = "CC0 1.0 Universal"
  val url = "https://creativecommons.org/publicdomain/zero/1.0/legalcode"
}

case object License_PDM extends License {
  val licenseType = "PDM"
  val label = "Public Domain Mark"
  val url = "https://creativecommons.org/share-your-work/public-domain/pdm/"
}

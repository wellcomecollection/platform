package uk.ac.wellcome.models

sealed trait License {
  val licenseType: String
  val label: String
  val url: String
  val ontologyType: String = "License"
}

object License extends Logging {
  implicit val licenseEncoder = Encoder.instance[License](
    license =>
      Json.obj(
        ("licenseType", Json.fromString(license.licenseType)),
        ("label", Json.fromString(license.label)),
        ("url", Json.fromString(license.url)),
        ("ontologyType", Json.fromString(license.ontologyType))
    ))

  implicit val licenseDecoder = Decoder.instance[License](cursor =>
    for {
      licenseType <- cursor.downField("licenseType").as[String]
    } yield {
      createLicense(licenseType)
  })

  def createLicense(licenseType: String): License = {
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

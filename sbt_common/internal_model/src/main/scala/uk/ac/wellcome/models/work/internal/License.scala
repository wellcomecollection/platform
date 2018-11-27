package uk.ac.wellcome.models.work.internal

import io.circe.{Decoder, Encoder, Json}

sealed trait License {
  val id: String
  val label: String
  val url: Option[String]
  val ontologyType: String = "License"
}

object License {
  implicit val licenseEncoder = Encoder.instance[License](
    license =>
      Json.obj(
        ("id", Json.fromString(license.id))
    )
  )

  implicit val licenseDecoder = Decoder.instance[License](cursor =>
    for {
      id <- cursor.downField("id").as[String]
    } yield {
      createLicense(id)
  })

  def createLicense(id: String): License = {
    id match {
      case s: String if s == License_CCBY.id     => License_CCBY
      case s: String if s == License_CCBYNC.id   => License_CCBYNC
      case s: String if s == License_CCBYNCND.id => License_CCBYNCND
      case s: String if s == License_CC0.id      => License_CC0
      case s: String if s == License_PDM.id      => License_PDM
      case s: String if s == License_CopyrightNotCleared.id =>
        License_CopyrightNotCleared
      case _ =>
        val errorMessage = s"$id is not a valid id"
        throw new Exception(errorMessage)
    }
  }
}

case object License_CCBY extends License {
  val id = "cc-by"
  val label = "Attribution 4.0 International (CC BY 4.0)"
  val url = Some("http://creativecommons.org/licenses/by/4.0/")
}

case object License_CCBYNC extends License {
  val id = "cc-by-nc"
  val label = "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)"
  val url = Some("https://creativecommons.org/licenses/by-nc/4.0/")
}

case object License_CCBYNCND extends License {
  val id = "cc-by-nc-nd"
  val label =
    "Attribution-NonCommercial-NoDerivatives 4.0 International (CC BY-NC-ND 4.0)"
  val url = Some("https://creativecommons.org/licenses/by-nc-nd/4.0/")
}

case object License_CC0 extends License {
  val id = "cc-0"
  val label = "CC0 1.0 Universal"
  val url = Some("https://creativecommons.org/publicdomain/zero/1.0/legalcode")
}

case object License_PDM extends License {
  val id = "pdm"
  val label = "Public Domain Mark"
  val url = Some(
    "https://creativecommons.org/share-your-work/public-domain/pdm/")
}

case object License_CopyrightNotCleared extends License {
  val id = "copyright-not-cleared"
  val label = "Copyright not cleared"
  val url: Option[String] = None
}

package uk.ac.wellcome.platform.api.models

case class Error(
  errorType: String,
  httpStatus: Option[Int] = None,
  label: String,
  description: Option[String] = None
) {
  val ontologyType: String = "Error"
}

case object Error {
  def apply(variant: String, description: Option[String]): Error = {
    variant match {
      case "http-400" =>
        Error(
          errorType = "http",
          httpStatus = Some(400),
          label = "Bad Request",
          description = description
        )
      case "http-404" =>
        Error(
          errorType = "http",
          httpStatus = Some(404),
          label = "Not Found",
          description = description
        )
      case "http-410" =>
        Error(
          errorType = "http",
          httpStatus = Some(410),
          label = "Gone",
          description = description
        )
      case "http-500" =>
        Error(
          errorType = "http",
          httpStatus = Some(500),
          label = "Internal Server Error",
          description = description
        )
      case unknownVariant =>
        throw new Exception(s"$unknownVariant is not a valid error variant")
    }
  }
}

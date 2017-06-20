package uk.ac.wellcome.platform.api.models

case class WorksIncludes(identifiers: Boolean = false)

case object WorksIncludes {

  def apply(queryParam: Option[String]): WorksIncludes = {
    queryParam match {
      case Some(s) => WorksIncludes(s)
      case None => WorksIncludes()
    }
  }

  /// Parse an ?includes query-parameter string.
  ///
  /// In this method, any unrecognised includes are simply ignored.
  def apply(queryParam: String): WorksIncludes = {
    val includesList = queryParam.split(",").toList
    WorksIncludes(
      identifiers = includesList.contains("identifiers")
    )
  }
}

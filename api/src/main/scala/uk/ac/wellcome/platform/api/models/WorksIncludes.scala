package uk.ac.wellcome.platform.api.models

case class WorksIncludes(identifiers: Boolean = false)

case object WorksIncludes {

  val recognisedIncludes = List("identifiers")

  /// Parse an ?includes query-parameter string.
  ///
  /// In this method, any unrecognised includes are simply ignored.
  def apply(queryParam: String): WorksIncludes = {
    val includesList = queryParam.split(",").toList
    WorksIncludes(
      identifiers = includesList.contains("identifiers")
    )
  }

  def apply(queryParam: Option[String]): WorksIncludes = {
    queryParam match {
      case Some(s) => WorksIncludes(s)
      case None => WorksIncludes()
    }
  }

  /// Validate and create an ?includes query-parameter string.
  ///
  /// This checks that every element in the query parameter is a known
  /// include before creating it.
  def create(queryParam: String): Either[List[String], WorksIncludes] = {
    val includesList = queryParam.split(",").toList
    val unrecognisedIncludes = includesList
      .filterNot(recognisedIncludes.contains)
    if (unrecognisedIncludes.length == 0) {
      Right(WorksIncludes(queryParam))
    } else {
      Left(unrecognisedIncludes)
    }
  }

  def create(queryParam: Option[String]): Either[List[String], WorksIncludes] = {
    queryParam match {
      case Some(s) => create(s)
      case None => Right(WorksIncludes())
    }
  }
}

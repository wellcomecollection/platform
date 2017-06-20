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

  /// Validate an ?includes query-parameter string.
  ///
  /// Specifically, this checks that every element in the query parameter
  /// is a known include.  Returns a Left if this is true, or a Right
  /// containing a list of any unrecognised includes.
  def validate(queryParam: String): Either[Unit, List[String]] = {
    val includesList = queryParam.split(",").toList
    val unrecognisedIncludes = includesList
      .filterNot(recognisedIncludes.contains)
    unrecognisedIncludes.length match {
      case 0 => Left(Unit)
      case _ => Right(unrecognisedIncludes)
    }
  }

  def validate(queryParam: Option[String]): Either[Unit, List[String]] = {
    queryParam match {
      case Some(s) => validate(s)
      case None => Left(Unit)
    }
  }
}

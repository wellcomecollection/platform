package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonMappingException}

case class WorksIncludes(identifiers: Boolean = false)

class IncludesParsingException(msg: String) extends JsonMappingException(msg: String)

case object WorksIncludes {

  val recognisedIncludes = List("identifiers")

  /// Parse an ?includes query-parameter string.
  ///
  /// In this method, any unrecognised includes are simply ignored.
  def apply(queryParam: String): WorksIncludes = {
    throw new IncludesParsingException("I haz a very sad")
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

class WorksIncludesDeserializer extends JsonDeserializer[WorksIncludes] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): WorksIncludes = {
    WorksIncludes(p.getText())
  }
}

class WorksIncludesDeserializerModule extends SimpleModule {
  addDeserializer(classOf[WorksIncludes], new WorksIncludesDeserializer())
}

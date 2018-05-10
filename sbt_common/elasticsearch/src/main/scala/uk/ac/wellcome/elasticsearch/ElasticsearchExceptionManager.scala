package uk.ac.wellcome.elasticsearch

import io.circe.optics.JsonPath._
import io.circe.parser._
import org.elasticsearch.client.ResponseException

import scala.util.{Failure, Success}

trait ElasticsearchExceptionManager {
  def getErrorType(responseException: ResponseException): Option[String] = {
    // Annoyingly, the exact format of message is
    //
    //    POST http://localhost:9200/path: HTTP/1.1 500 Internal Server Error
    //    {"error":{...}}
    //
    // so we need to read the second line to get the actual JSON.
    //
    // Except when it isn't!  Sometimes these exceptions don't include
    // a JSON response, so there's only a single-line message -- then `.head`
    // fails: "NoSuchElementException: next on empty iterator".  I think
    // this happens when the cluster doesn't reply, but I'm not sure.
    val maybeJsonDocument = responseException.getMessage
      .split("\n")
      .tail
      .headOption

    maybeJsonDocument.flatMap { jsonDocument =>
      parse(jsonDocument).toTry match {
        case Success(jsonObject) =>
          root.error.`type`.string.getOption(jsonObject)
        case Failure(_) => None
      }
    }
  }
}

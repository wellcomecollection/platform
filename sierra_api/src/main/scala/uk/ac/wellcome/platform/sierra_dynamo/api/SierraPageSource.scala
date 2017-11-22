package uk.ac.wellcome.platform.sierra_dynamo.api

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import io.circe.Json
import io.circe.optics.JsonPath.root
import io.circe.parser.parse
import org.slf4j.{Logger, LoggerFactory}

import scalaj.http.{Http, HttpResponse}

private[api] class SierraPageSource(apiUrl: String, oauthKey: String, oauthSecret: String)(
  resourceType: String,
  params: Map[String, String] = Map.empty)
    extends GraphStage[SourceShape[List[Json]]] {

  val out: Outlet[List[Json]] = Outlet("SierraSource")

  override def shape = SourceShape(out)
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      var token: String = refreshToken(apiUrl, oauthKey, oauthSecret)
      var lastId: Option[Int] = None
      var jsonList: List[Json] = Nil

      setHandler(out,
        new OutHandler {
          override def onPull(): Unit = makeSierraRequestAndPush()
        }
      )

      private def makeSierraRequestAndPush(): Unit = {
        val newParams = lastId match {
          case Some(id) =>
            params + ("id" -> s"[${id + 1},]")
          case None => params
        }

        makeRequestWith(newParams, ifUnauthorized = {
          token = refreshToken(apiUrl, oauthKey, oauthSecret)
          makeRequestWith(newParams, ifUnauthorized = {
            fail(out, new RuntimeException("Unauthorized!"))
          })
        })
      }

      private def makeRequestWith[T](newParams: Map[String, String], ifUnauthorized: => Unit): Unit = {
        val newResponse = makeRequest(apiUrl, resourceType, token, newParams)

        newResponse.code match {
          case 200 => refreshJsonListAndPush(newResponse)
          case 404 => complete(out)
          case 401 => ifUnauthorized
          case _ => fail(out, new RuntimeException("Unexpected Error"))
        }
      }

      private def refreshJsonListAndPush(response: HttpResponse[String]): Unit = {
        val responseJson = parse(response.body).right.getOrElse(
          throw new RuntimeException("response was not json"))

        jsonList = root.entries.each.json.getAll(responseJson)

        lastId = Some(
          root.id.string
            .getOption(jsonList.last)
            .getOrElse(
              throw new RuntimeException("id not found in item"))
            .toInt)

        push(out, jsonList)
      }

      private def refreshToken(apiUrl: String,
                               oauthKey: String,
                               oauthSecret: String) = {
        val tokenResponse =
          Http(s"$apiUrl/token").postForm.auth(oauthKey, oauthSecret).asString
        val json = parse(tokenResponse.body).right
          .getOrElse(throw new RuntimeException("response was not json"))
        root.access_token.string.getOption(json).getOrElse(
          throw new Exception("Failed to refresh token!")
        )
      }


    }

  private def makeRequest(apiUrl: String,
                          resourceType: String,
                          token: String,
                          params: Map[String, String]) = {

    logger.debug(s"Making request with parameters $params & token $token")

    Http(s"$apiUrl/$resourceType")
      .params(params)
      .header("Authorization", s"Bearer $token")
      .header("Accept", "application/json")
      .asString
  }
}

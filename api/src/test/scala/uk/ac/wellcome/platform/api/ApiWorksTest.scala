package uk.ac.wellcome.platform.api

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

class ApiWorksTest extends FeatureTest with IndexedElasticSearchLocal {

  implicit val jsonMapper = IdentifiedWork
  override val server =
    new EmbeddedHttpServer(
      new Server,
      flags = Map(
        "es.host" -> "localhost",
        "es.port" -> 9300.toString,
        "es.name" -> "wellcome",
        "es.xpack.enabled" -> "true",
        "es.xpack.user" -> "elastic:changeme",
        "es.xpack.sslEnabled" -> "false",
        "es.sniff" -> "false"
      )
    )

  val canonicalId = "1234"
  val label = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"

  val period = Period("the past")
  val agent = Agent("a person")

  test("it should return a list of works") {

    val works = (1 to 3).map(
      (idx: Int) =>
        identifiedWorkWith(
          canonicalId = s"${idx}-${canonicalId}",
          label = s"${idx}-${label}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = period.copy(label = s"${idx}-${period.label}"),
          creator = agent.copy(label = s"${idx}-${agent.label}")
      ))

    works.map(insertIntoElasticSearch)

    eventually {
      server.httpGet(
        path = "/catalogue/v0/works",
        andExpect = Status.Ok,
        withJsonBody = s"""
            |{
            |  "@context": "http://localhost:8888/catalogue/v0/context.json",
            |  "type": "ResultList",
            |  "pageSize": 10,
            |  "results": [
            |   {
            |     "type": "Work",
            |     "id": "${works(0).canonicalId}",
            |     "label": "${works(0).work.label}",
            |     "description": "${works(0).work.description.get}",
            |     "lettering": "${works(0).work.lettering.get}",
            |     "hasCreatedDate": {
            |       "type": "Period",
            |       "label": "${works(0).work.hasCreatedDate.get.label}"
            |     },
            |     "hasCreator": [{
            |       "type": "Agent",
            |       "label": "${works(0).work.hasCreator(0).label}"
            |     }]
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${works(1).canonicalId}",
            |     "label": "${works(1).work.label}",
            |     "description": "${works(1).work.description.get}",
            |     "lettering": "${works(1).work.lettering.get}",
            |     "hasCreatedDate": {
            |       "type": "Period",
            |       "label": "${works(1).work.hasCreatedDate.get.label}"
            |     },
            |     "hasCreator": [{
            |       "type": "Agent",
            |       "label": "${works(1).work.hasCreator(0).label}"
            |     }]
            |   },
            |   {
            |     "type": "Work",
            |     "id": "${works(2).canonicalId}",
            |     "label": "${works(2).work.label}",
            |     "description": "${works(2).work.description.get}",
            |     "lettering": "${works(2).work.lettering.get}",
            |     "hasCreatedDate": {
            |       "type": "Period",
            |       "label": "${works(2).work.hasCreatedDate.get.label}"
            |     },
            |     "hasCreator": [{
            |       "type": "Agent",
            |       "label": "${works(2).work.hasCreator(0).label}"
            |     }]
            |   }
            |  ]
            |}
          """.stripMargin
      )
    }
  }

  test("it should return a single work when requested with id") {
    val identifiedWork =
      identifiedWorkWith(
        canonicalId = canonicalId,
        label = label,
        description = description,
        lettering = lettering,
        createdDate = period,
        creator = agent
      )
    insertIntoElasticSearch(identifiedWork)

    eventually {
      server.httpGet(
        path = s"/catalogue/v0/works/$canonicalId",
        andExpect = Status.Ok,
        withJsonBody = s"""
            |{
            | "@context": "http://localhost:8888/catalogue/v0/context.json",
            | "type": "Work",
            | "id": "$canonicalId",
            | "label": "$label",
            | "description": "$description",
            | "lettering": "$lettering",
            | "hasCreatedDate": {
            |   "type": "Period",
            |   "label": "${period.label}"
            | },
            | "hasCreator": [{
            |   "type": "Agent",
            |   "label": "${agent.label}"
            | }]
            |}
          """.stripMargin
      )
    }
  }

  test(
    "it should return a not found error when requesting a single work with a non existing id") {
    server.httpGet(
      path = "/catalogue/v0/works/non-existing-id",
      andExpect = Status.NotFound,
      withJsonBody = ""
    )
  }

  private def insertIntoElasticSearch(identifiedWork: IdentifiedWork): Any = {
    elasticClient.execute(
      indexInto(indexName / itemType)
        .id(identifiedWork.canonicalId)
        .doc(identifiedWork))
  }
  private def identifiedWorkWith(canonicalId: String, label: String) = {
    IdentifiedWork(canonicalId,
                   Work(identifiers =
                          List(SourceIdentifier("Miro", "MiroID", "5678")),
                        label = label))

  }
  private def identifiedWorkWith(canonicalId: String,
                                 label: String,
                                 description: String,
                                 lettering: String,
                                 createdDate: Period,
                                 creator: Agent) = {

    IdentifiedWork(
      canonicalId = canonicalId,
      work = Work(
        identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")),
        label = label,
        description = Some(description),
        lettering = Some(lettering),
        hasCreatedDate = Some(createdDate),
        hasCreator = List(creator)
      )
    )
  }
}

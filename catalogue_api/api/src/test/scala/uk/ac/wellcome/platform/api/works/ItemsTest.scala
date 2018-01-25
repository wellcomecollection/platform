package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.{Item, Work}

class ItemsTest extends ApiWorksTestBase {

  it("does not include invisible items in the response") {
    val visibleItem = Item(
      canonicalId = Some("ban3ynzy"),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      locations = List(),
      visible = true
    )
    val invisibleItem = Item(
      canonicalId = Some("bvnxbcqr"),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      locations = List(),
      visible = false
    )

    val work = Work(
      title = Some("Bermudans basking with blue beavers"),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      canonicalId = Some("bw9b9da4"),
      items = List(visibleItem, invisibleItem)
    )

    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works?includes=items",
        andExpect = Status.Ok,
        withJsonBody = s"""
          |{
          |  ${resultList(totalResults = 1)},
          |  "results": [
          |    {
          |      "type": "Work",
          |      "id": "${work.id}",
          |      "title": "${work.title.get}",
          |      "creators": [ ],
          |      "subjects": [ ],
          |      "genres": [ ],
          |      "publishers": [ ],
          |      "items": ${items(List(visibleItem))}
          |    }
          |  ]
          |}
          """.stripMargin
      )
    }
  }
}

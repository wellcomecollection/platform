package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models._

class LocationsTest extends ApiWorksTestBase {
  it("should render a physical location correctly") {
    withLocalElasticsearchIndex(itemType = itemType) { indexName =>
      val flags = esLocalFlags(indexName, itemType)
      withServer(flags) { server =>
        val physicalLocation: Location =
          PhysicalLocation(
            locationType = "smeg",
            label = "a stack of slick slimes")
        val work = IdentifiedWork(
          canonicalId = "zm9q6c6h",
          sourceIdentifier = sourceIdentifier,
          version = 1,
          title = Some("A zoo of zebras doing zumba"),
          items = List(
            IdentifiedItem(
              canonicalId = "mhberjwy7",
              sourceIdentifier = sourceIdentifier,
              locations = List(physicalLocation)))
        )

        insertIntoElasticsearch(indexName, itemType, work)

        eventually {
          server.httpGet(
            path = s"/$apiPrefix/works/${work.canonicalId}?includes=items",
            andExpect = Status.Ok,
            withJsonBody = s"""
                              |{
                              | "@context": "https://localhost:8888/$apiPrefix/context.json",
                              | "type": "Work",
                              | "id": "${work.canonicalId}",
                              | "title": "${work.title.get}",
                              | "creators": [ ],
                              | "items": [ ${items(work.items)} ],
                              | "subjects": [ ],
                              | "genres": [ ],
                              | "publishers": [ ],
                              | "placesOfPublication": [ ]
                              |}
              """.stripMargin
          )
        }
      }
    }
  }
}

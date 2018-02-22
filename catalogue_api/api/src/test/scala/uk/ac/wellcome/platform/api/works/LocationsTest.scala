package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models._

class LocationsTest extends ApiWorksTestBase {
  it("should render a physical location correctly") {
    val physicalLocation: Location =
      PhysicalLocation(locationType = "smeg",
                       label = "a stack of slick slimes")
    val work = IdentifiedWork(
      canonicalId = Some("zm9q6c6h"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("A zoo of zebras doing zumba"),
      items = List(
        Item(canonicalId = Some("mhberjwy7"),
             sourceIdentifier = sourceIdentifier,
             locations = List(physicalLocation)))
    )

    insertIntoElasticSearch(work)

    eventually {
      server.httpGet(
        path = s"/$apiPrefix/works/${work.canonicalId.get}?includes=items",
        andExpect = Status.Ok,
        withJsonBody = s"""
                          |{
                          | "@context": "https://localhost:8888/$apiPrefix/context.json",
                          | "type": "Work",
                          | "id": "${work.canonicalId.get}",
                          | "title": "${work.title.get}",
                          | "creators": [ ],
                          | "items": [ ${items(work.items)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ]
                          |}
          """.stripMargin
      )
    }
  }
}

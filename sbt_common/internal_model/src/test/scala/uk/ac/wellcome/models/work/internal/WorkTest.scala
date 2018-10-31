package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators

class WorkTest extends FunSpec with Matchers with IdentifiersGenerators {

  // This is based on a real failure.  We deployed a version of the API
  // with a newer model than was in Elasticsearch -- in particular, it had
  // a newer Item definition.
  //
  // The result was a V1 API which didn't include any items, which caused
  // issues for /works pages on wellcomecollection.org.  This test checks
  // for "strictness" in our JSON parsing.
  //
  // The problem is that the "items" field has an outdated model -- it
  // should be wrapped in an "Identified" block, with the item data
  // inside an "agent" field.
  //
  it("fails to parse Work JSON with an outdated Item definition") {
    val jsonString =
      s"""
        |{
        |  "canonicalId": "$createCanonicalId",
        |  "sourceIdentifier": ${toJson(createSourceIdentifier).get},
        |  "otherIdentifiers": [],
        |  "mergeCandidates": [],
        |  "title": "${randomAlphanumeric(length = 10)}",
        |  "workType": null,
        |  "description": null,
        |  "physicalDescription": null,
        |  "extent": null,
        |  "lettering": null,
        |  "createdDate": null,
        |  "subjects": [],
        |  "genres": [],
        |  "contributors": [],
        |  "thumbnail": null,
        |  "production": [],
        |  "language": null,
        |  "dimensions": null,
        |  "items": [
        |    {
        |      "canonicalId": "$createCanonicalId",
        |      "sourceIdentifier": ${toJson(createSourceIdentifier).get},
        |      "otherIdentifiers": [],
        |      "locations": [],
        |      "ontologyType": "Item"
        |    }
        |  ],
        |  "version": 1,
        |  "visible": true,
        |  "ontologyType": "Work"
        |}
      """.stripMargin

    val caught = intercept[JsonDecodingError] {
      fromJson[IdentifiedWork](jsonString).get
    }
    caught.getMessage should startWith(
      "Attempt to decode value on failed cursor")
  }
}

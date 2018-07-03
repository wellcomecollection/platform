package uk.ac.wellcome.models.work.internal

import org.scalatest.FunSpec
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.utils.JsonUtil._

class WorkTest extends FunSpec {

  // This is based on a real failure.  We deployed a version of the API
  // with a newer model than was in Elasticsearch -- in particular, it had
  // a newer Item definition.
  //
  // The result was a V1 API which didn't include any items, which caused
  // issues for /works pages on wellcomecollection.org.  This test checks
  // for "strictness" in our JSON parsing.
  //
  it("fails to parse Work JSON with an outdated Item definition") {
    val jsonString =
      """
        |{
        |  "canonicalId": "sgmzn6pu",
        |  "sourceIdentifier": {
        |    "identifierType": {
        |      "id": "miro-image-number",
        |      "label": "Miro image number",
        |      "ontologyType": "IdentifierType"
        |    },
        |    "ontologyType": "Work",
        |    "value": "L0057464"
        |  },
        |  "otherIdentifiers": [
        |    {
        |      "identifierType": {
        |        "id": "miro-library-reference",
        |        "label": "Miro library reference",
        |        "ontologyType": "IdentifierType"
        |      },
        |      "ontologyType": "Work",
        |      "value": "Science Museum A113324|CLAC100116"
        |    }
        |  ],
        |  "mergeCandidates": [],
        |  "title": "Adult human mummy in sarcophagus, 323BC-31AD. Photographed on display in the Upper Wellcome Gallery of the Science Museum",
        |  "workType": null,
        |  "description": null,
        |  "physicalDescription": null,
        |  "extent": null,
        |  "lettering": null,
        |  "createdDate": null,
        |  "subjects": [],
        |  "genres": [],
        |  "contributors": [
        |    {
        |      "agent": {
        |        "agent": {
        |          "label": "Science Museum, London",
        |          "type": "Agent"
        |        },
        |        "type": "Unidentifiable"
        |      },
        |      "roles": [],
        |      "ontologyType": "Contributor"
        |    }
        |  ],
        |  "thumbnail": {
        |    "url": "https://iiif.wellcomecollection.org/image/L0057464.jpg/full/300,/0/default.jpg",
        |    "license": {
        |      "id": "cc-by",
        |      "label": "Attribution 4.0 International (CC BY 4.0)",
        |      "url": "http://creativecommons.org/licenses/by/4.0/",
        |      "ontologyType": "License"
        |    },
        |    "locationType": {
        |      "id": "thumbnail-image",
        |      "label": "Thumbnail Image",
        |      "ontologyType": "LocationType"
        |    },
        |    "credit": null,
        |    "ontologyType": "DigitalLocation",
        |    "type": "DigitalLocation"
        |  },
        |  "production": [],
        |  "language": null,
        |  "dimensions": null,
        |  "items": [
        |    {
        |      "canonicalId": "hy2cbxkh",
        |      "sourceIdentifier": {
        |        "identifierType": {
        |          "id": "miro-image-number",
        |          "label": "Miro image number",
        |          "ontologyType": "IdentifierType"
        |        },
        |        "ontologyType": "Item",
        |        "value": "L0057464"
        |      },
        |      "otherIdentifiers": [],
        |      "locations": [
        |        {
        |          "url": "https://iiif.wellcomecollection.org/image/L0057464.jpg/info.json",
        |          "license": {
        |            "id": "cc-by",
        |            "label": "Attribution 4.0 International (CC BY 4.0)",
        |            "url": "http://creativecommons.org/licenses/by/4.0/",
        |            "ontologyType": "License"
        |          },
        |          "locationType": {
        |            "id": "iiif-image",
        |            "label": "IIIF image",
        |            "ontologyType": "LocationType"
        |          },
        |          "credit": "Science Museum, London",
        |          "ontologyType": "DigitalLocation",
        |          "type": "DigitalLocation"
        |        }
        |      ],
        |      "ontologyType": "Item"
        |    }
        |  ],
        |  "version": 27,
        |  "visible": true,
        |  "ontologyType": "Work"
        |}
      """.stripMargin

    intercept[GracefulFailureException] {
      fromJson[IdentifiedWork](jsonString).get
    }
  }
}

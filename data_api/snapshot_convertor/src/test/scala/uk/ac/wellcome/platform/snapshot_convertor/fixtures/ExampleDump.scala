package uk.ac.wellcome.platform.snapshot_convertor.fixtures

import uk.ac.wellcome.test.fixtures.S3
import org.scalatest.compatible.Assertion

trait ExampleDump extends S3 {

  val expectedDisplayWork =
    """
    {
       "id":"uhwweqqu",
       "title":"Handbuch der acuten Infectionskrankheiten. 1. Theil",
       "description":"description",
       "lettering":"lettering",
       "created_date":{
          "label":"1776",
          "type":"Period"
       },
       "creators":[
          {
             "label":"Pietro Fabris",
             "type":"Agent"
          }
       ],
       "identifiers":[
          {
             "identifier_scheme":"sierra-system-number",
             "value":"b1366502",
             "type":"Identifier"
          }
       ],
       "subjects":[
          {
             "label":"Volcano",
             "type":"Concept"
          }
       ],
       "genres":[
          {
             "label":"Scientific illustrations",
             "type":"Concept"
          }
       ],
       "thumbnail":{
          "location_type":"thumbnail-image",
          "url":"https://iiif.wellcomecollection.org/image/V0025257.jpg/full/300,/0/default.jpg",
          "license":{
             "license_type":"CC-BY",
             "label":"Attribution 4.0 International (CC BY 4.0)",
             "url":"http://creativecommons.org/licenses/by/4.0/",
             "type":"License"
          },
          "type":"DigitalLocation"
       },
       "items":[
          {
             "id":"a5s73nk7",
             "identifiers":[
                {
                   "identifier_scheme":"miro-image-number",
                   "value":"V0025257",
                   "type":"Identifier"
                }
             ],
             "locations":[
                {
                   "location_type":"iiif-image",
                   "url":"https://iiif.wellcomecollection.org/image/V0025257.jpg/info.json",
                   "credit":"Wellcome Collection",
                   "license":{
                      "license_type":"CC-BY",
                      "label":"Attribution 4.0 International (CC BY 4.0)",
                      "url":"http://creativecommons.org/licenses/by/4.0/",
                      "type":"License"
                   },
                   "type":"DigitalLocation"
                }
             ],
             "type":"Item"
          }
       ],
       "publishers":[
          {
             "label":"F.C.W. Vogel,",
             "type":"Organisation"
          }
       ],
       "places_of_publication":[

       ],
       "type":"Work"
    }"""

  val elasticDump = getClass.getResource("/elasticdump_example.txt.gz")

  def withExampleDump(bucketName: String) =
    withLocalS3ObjectFromResource[Assertion](bucketName, elasticDump) _

}

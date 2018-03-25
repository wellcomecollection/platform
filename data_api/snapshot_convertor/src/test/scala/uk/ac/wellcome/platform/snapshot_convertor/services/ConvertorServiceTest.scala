package uk.ac.wellcome.platform.snapshot_convertor.services

import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

import com.amazonaws.services.s3.model.ObjectMetadata
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.snapshot_convertor.models.ConversionJob
import uk.ac.wellcome.test.fixtures.{AkkaFixtures, TestWith}

import io.circe.parser._
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.ConvertorServiceFixture
import uk.ac.wellcome.test.utils.ExtendedPatience

class ConvertorServiceTest
    extends FunSpec
    with ScalaFutures
    with AkkaFixtures
    with Matchers
    with ExtendedPatience
    with ConvertorServiceFixture {

  def withCompressedTestDump[R](bucketName: String)(
    testWith: TestWith[String, R]): R = {

    val key = "elasticdump_example.txt.gz"

    val input = getClass.getResourceAsStream(s"/$key")
    val metadata = new ObjectMetadata()

    s3Client.putObject(bucketName, key, input, metadata)

    testWith(key)
  }

  it(
    "converts a gzipped elasticdump from S3 into the correct format in the target bucket") {

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

    withConvertorService { fixtures =>
      withCompressedTestDump(fixtures.bucketName) { key =>
        val conversionJob = ConversionJob(
          bucketName = fixtures.bucketName,
          objectKey = key
        )

        val future = fixtures.convertorService.runConversion(conversionJob)

        whenReady(future) { completedConversionJob =>
          val inputStream = s3Client
            .getObject(fixtures.bucketName, "target.txt.gz")
            .getObjectContent

          val actualDisplayWorkJson = parse(
            scala.io.Source
              .fromInputStream(
                new GZIPInputStream(new BufferedInputStream(inputStream)))
              .mkString
              .split("\n")
              .head
          )

          actualDisplayWorkJson should be(parse(expectedDisplayWork))
        }

      }
    }
  }
}

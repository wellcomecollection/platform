package uk.ac.wellcome.transformer

import com.gu.scanamo.Scanamo
import com.twitter.inject.Injector
import uk.ac.wellcome.test.utils.IntegrationTestBase

class TransformerIntegrationTest extends IntegrationTestBase{
  override def injector: Injector = ???

  test("it should transform Miro data into Unified items and push them into the id_minter SNS topic"){
    Scanamo.put(dynamoDbClient)("MiroData")(MiroData("1234", "Images-A", """{"image-title": "some image title"}"""))


  }
}

case class MiroData(MiroID: String, MiroCollection: String, data: String)

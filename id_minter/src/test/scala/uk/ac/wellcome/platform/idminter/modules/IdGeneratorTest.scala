package uk.ac.wellcome.platform.idminter.modules

import org.scalatest.FunSpec

class IdGeneratorTest extends FunSpec with DynamoDBLocal {

  it("should search the miro id in dynamoDb and return the canonical id if it finds it"){
    val idGenerator = new IdGenerator(dynamoDbClient)


  }
}

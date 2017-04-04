package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner
import org.scalatest.{BeforeAndAfterEach, Suite}

trait DynamoDBLocal extends Suite with BeforeAndAfterEach {

  val port = 45678
  val dynamoDbClient = new AmazonDynamoDBClient(new BasicAWSCredentials("access", "secret"))
  dynamoDbClient.setEndpoint("http://localhost:" + port)
  val server = ServerRunner.createServerFromCommandLineArgs(Array("-inMemory", "-port", port.toString))

  override def beforeEach() = {
    server.start()
  }

  override def afterEach(): Unit = {
    server.stop()
  }
}

package uk.ac.wellcome.platform.calm_adapter.modules

import com.amazonaws.services.dynamodbv2._
import com.twitter.inject.{Injector, Logging, TwitterModule}

import uk.ac.wellcome.platform.finatra.modules._
import uk.ac.wellcome.utils._


object DynamoWarmupModule extends TwitterModule {
  override val modules = Seq(
    DynamoClientModule)

  def modifyCapacity(dynamoClient: AmazonDynamoDB, capacity: Long) = try {

    val capacityModifier = new DynamoUpdateWriteCapacityCapable {
      val client = dynamoClient
    }
    info(s"Setting write capacity of CalmData table to ${capacity}")

    capacityModifier.updateWriteCapacity("CalmData", capacity)
  } catch {
    case e: Throwable => error(s"Error in modifyCapacity(): ${e}")
  }

  override def singletonStartup(injector: Injector) {
    modifyCapacity(injector.instance[AmazonDynamoDB], 3)
  }

  override def singletonShutdown(injector: Injector) {
    modifyCapacity(injector.instance[AmazonDynamoDB], 1)
  }
}

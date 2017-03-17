package uk.ac.wellcome.platform.calm_adapter.modules

import com.amazonaws.services.dynamodbv2._
import com.twitter.inject.{Injector, Logging, TwitterModule}

import uk.ac.wellcome.platform.finatra.modules._
import uk.ac.wellcome.utils._

object DynamoWarmupModule extends TwitterModule {
  override val modules = Seq(DynamoClientModule, DynamoConfigModule)

  val writeCapacity =
    flag(
      name = "writeCapacity",
      default = 600L,
      help = "Dynamo write capacity"
    )

  def modifyCapacity(
    dynamoClient: AmazonDynamoDB,
    dynamoConfig: DynamoConfig,
    capacity: Long = 1L
  ) =
    try {

      (new DynamoUpdateWriteCapacityCapable {
        val client = dynamoClient
      }).updateWriteCapacity(dynamoConfig.table, capacity)

      info(
        s"Setting write capacity of ${dynamoConfig.table} table to ${capacity}")
    } catch {
      case e: Throwable => error(s"Error in modifyCapacity(): ${e}")
    }

  override def singletonStartup(injector: Injector) =
    modifyCapacity(injector.instance[AmazonDynamoDB],
                   injector.instance[DynamoConfig],
                   writeCapacity())

  override def singletonShutdown(injector: Injector) =
    modifyCapacity(injector.instance[AmazonDynamoDB],
                   injector.instance[DynamoConfig],
                   1L)
}

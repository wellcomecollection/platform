package uk.ac.wellcome.platform.calm_adapter.modules

import com.amazonaws.services.dynamodbv2._
import com.twitter.app.Flag
import uk.ac.wellcome.models.aws.DynamoConfig
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.finatra.annotations.CalmDynamoConfig
import uk.ac.wellcome.finatra.modules.{
  AWSConfigModule,
  DynamoClientModule,
  DynamoConfigModule
}
import uk.ac.wellcome.utils._

/** Scale up/down Dynamo write capacity while the adapter is running.
  *
  * Dynamo pricing is based on "provisioned read/write throughput capacity" --
  * essentially, how fast you can read/write the database.  Faster writes are
  * more expensive.  Details: https://aws.amazon.com/dynamodb/pricing/
  *
  * When the adapter is running, we want lots of write capacity so that we can
  * finish quickly.  But most of the time that write capacity isn't being used,
  * so leaving it up would be a waste of money.
  *
  * This actor increases our write capacity at the start of a run, then resets
  * it to 1 (the lowest value) after we're finished, so we're only running with
  * a high write capacity for as short a period as possible.
  */
object DynamoWarmupModule extends TwitterModule {
  override val modules =
    Seq(AWSConfigModule, DynamoClientModule, DynamoConfigModule)

  val writeCapacity: Flag[Long] =
    flag(
      name = "writeCapacity",
      default = 5L,
      help = "Dynamo write capacity"
    )

  def modifyCapacity(
    dynamoClient: AmazonDynamoDB,
    dynamoConfig: DynamoConfig,
    capacity: Long = 1L
  ): Unit =
    try {

      if (dynamoConfig.table == "") {
        error("DynamoDB table name must not be empty")
      }

      new DynamoUpdateWriteCapacityCapable {
        val client: AmazonDynamoDB = dynamoClient
      }.updateWriteCapacity(dynamoConfig.table, capacity)

      info(
        s"Setting write capacity of ${dynamoConfig.table} table to $capacity")
    } catch {
      case e: Throwable => error(s"Error in modifyCapacity(): $e")
    }

  override def singletonStartup(injector: Injector): Unit = {
    modifyCapacity(injector.instance[AmazonDynamoDB],
                   injector.instance[DynamoConfig, CalmDynamoConfig],
                   writeCapacity())

  }

  override def singletonShutdown(injector: Injector): Unit =
    modifyCapacity(injector.instance[AmazonDynamoDB],
                   injector.instance[DynamoConfig, CalmDynamoConfig],
                   1L)
}

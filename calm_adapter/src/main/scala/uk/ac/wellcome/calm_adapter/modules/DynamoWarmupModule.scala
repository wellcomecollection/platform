package uk.ac.wellcome.platform.calm_adapter.modules

import com.amazonaws.services.dynamodbv2._
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.finatra.modules.{AWSConfigModule, DynamoClientModule}
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
    Seq(AWSConfigModule, DynamoClientModule)

  val writeCapacity: Flag[Long] =
    flag(
      name = "aws.dynamo.warmup.writeCapacity",
      default = 5L,
      help = "Dynamo write capacity"
    )

  val tableToWarm: Flag[String] =
    flag(
      name = "aws.dynamo.warmup.tableName",
      default = "CalmData",
      help = "Dynamo table to target with write capacity increase"
    )

  def modifyCapacity(
    dynamoClient: AmazonDynamoDB,
    capacity: Long = 1L
  ): Unit =
    try {
      new DynamoUpdateWriteCapacityCapable {
        val client: AmazonDynamoDB = dynamoClient
      }.updateWriteCapacity(tableToWarm(), capacity)

      info(s"Setting write capacity of ${tableToWarm()} table to $capacity")
    } catch {
      case e: Throwable => error(s"Error in modifyCapacity(): $e")
    }

  override def singletonStartup(injector: Injector): Unit =
    modifyCapacity(injector.instance[AmazonDynamoDB], writeCapacity())

  override def singletonShutdown(injector: Injector): Unit =
    modifyCapacity(injector.instance[AmazonDynamoDB], 1L)
}

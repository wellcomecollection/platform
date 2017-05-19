package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.finatra.annotations.{CalmDynamoConfig, MiroDynamoConfig, IdentifiersDynamoConfig}
import uk.ac.wellcome.models.aws.DynamoConfig

object DynamoConfigModule extends TwitterModule {
  val applicationNameTpl: String = "aws.dynamo.%s.streams.appName"
  val arnTpl: String = "aws.dynamo.%s.streams.arn"
  val tableTpl: String = "aws.dynamo.%s.tableName"

  def appNameFlagId(tableName: String): String = applicationNameTpl.format(tableName)
  def arnFlagId(tableName: String): String = arnTpl.format(tableName)
  def tableFlagId(tableName: String): String = tableTpl.format(tableName)


  val miroTableName = "miroData"

  val miroApplicationName: Flag[String] = flag[String](appNameFlagId(miroTableName), "", "Name of the Kinesis app")
  val miroArn: Flag[String] = flag[String](arnFlagId(miroTableName), "", "ARN of the DynamoDB stream")
  val miroTable: Flag[String] = flag[String](tableFlagId(miroTableName), "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  @MiroDynamoConfig
  def providesMiroDynamoConfig(): DynamoConfig =
    DynamoConfig(miroApplicationName(), miroArn(), miroTable())

  val identifiersTableName = "identifiers"

  val identifiersApplicationName: Flag[String] =
    flag[String](appNameFlagId(identifiersTableName), "", "Name of the Kinesis app")
  val identifiersArn: Flag[String] = flag[String](arnFlagId(identifiersTableName), "", "ARN of the DynamoDB stream")
  val identifiersTable: Flag[String] = flag[String](tableFlagId(identifiersTableName), "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  @IdentifiersDynamoConfig
  def providesIdentifierDynamoConfig(): DynamoConfig =
    DynamoConfig(identifiersApplicationName(), identifiersArn(), identifiersTable())

  val calmTableName = "calmData"

  val calmApplicationName: Flag[String] =
    flag[String](appNameFlagId(calmTableName), "", "Name of the Kinesis app")
  val calmArn: Flag[String] = flag[String](arnFlagId(calmTableName), "", "ARN of the DynamoDB stream")
  val calmTable: Flag[String] = flag[String](tableFlagId(calmTableName), "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  @CalmDynamoConfig
  def providesCalmDynamoConfig(): DynamoConfig =
    DynamoConfig(calmApplicationName(), calmArn(), calmTable())
}

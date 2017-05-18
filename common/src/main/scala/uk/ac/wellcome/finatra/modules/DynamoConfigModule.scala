package uk.ac.wellcome.finatra.modules

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.aws.DynamoConfig

import com.google.inject.BindingAnnotation
import java.lang.annotation.Target
import java.lang.annotation.Retention
import java.lang.annotation.ElementType.PARAMETER
import java.lang.annotation.RetentionPolicy.RUNTIME

@BindingAnnotation
@Target(Array(PARAMETER))
@Retention(RUNTIME) class MiroDynamoConfig {}

@BindingAnnotation
@Target(Array(PARAMETER))
@Retention(RUNTIME) class IdentifierDynamoConfig {}

@BindingAnnotation
@Target(Array(PARAMETER))
@Retention(RUNTIME) class CalmDynamoConfig {}

trait DynamoConfigModule extends TwitterModule {
  val tableName: String

  val applicationName: Flag[String]
  val arn: Flag[String]
  val table: Flag[String]

  val applicationNameTpl: String = "aws.dynamo.%s.streams.appName"
  val arnTpl: String = "aws.dynamo.%s.streams.arn"
  val tableTpl: String = "aws.dynamo.%s.tableName"

  def appNameFlagId = applicationNameTpl.format(tableName)
  def arnFlagId = arnTpl.format(tableName)
  def tableFlagId = tableTpl.format(tableName)
}

object MiroTableDynamoConfigModule extends DynamoConfigModule {
  val tableName = "miroData"

  val applicationName =
    flag[String](appNameFlagId, "", "Name of the Kinesis app")
  val arn = flag[String](arnFlagId, "", "ARN of the DynamoDB stream")
  val table = flag[String](tableFlagId, "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  @MiroDynamoConfig
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(applicationName(), arn(), table())
}

object IdentifierTableDynamoConfigModule extends DynamoConfigModule {
  val tableName = "identifiers"

  val applicationName =
    flag[String](appNameFlagId, "", "Name of the Kinesis app")
  val arn = flag[String](arnFlagId, "", "ARN of the DynamoDB stream")
  val table = flag[String](tableFlagId, "", "Name of the DynamoDB table")

  @Singleton
  @Provides
  @IdentifierDynamoConfig
  def providesDynamoConfig(): DynamoConfig =
    DynamoConfig(applicationName(), arn(), table())
}

object CalmTableDynamoConfigModule extends DynamoConfigModule {
  val tableName = "calmData"

  @Singleton
  @Provides
  @CalmDynamoConfig
  val applicationName =
    flag[String](appNameFlagId, "", "Name of the Kinesis app")
  val arn = flag[String](arnFlagId, "", "ARN of the DynamoDB stream")
  val table = flag[String](tableFlagId, "", "Name of the DynamoDB table")
}

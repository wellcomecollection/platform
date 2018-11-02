package uk.ac.wellcome.platform.archive.common.progress.modules

import uk.ac.wellcome.platform.archive.common.modules.DynamoClientConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

case class ProgressTrackerConfig(dynamoConfig: DynamoConfig,
                                 dynamoClientConfig: DynamoClientConfig)

package uk.ac.wellcome.platform.archive.common.config.models

import uk.ac.wellcome.platform.archive.common.modules.DynamoClientConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

case class ProgressMonitorConfig(dynamoConfig: DynamoConfig,
                                 dynamoClientConfig: DynamoClientConfig)

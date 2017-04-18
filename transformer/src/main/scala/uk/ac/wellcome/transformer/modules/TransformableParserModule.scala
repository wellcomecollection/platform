package uk.ac.wellcome.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.Transformable
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.transformer.parsers.{CalmParser, MiroParser, TransformableParser}

object TransformableParserModule extends TwitterModule {

  @Singleton
  @Provides
  def providesTransformableParser(dynamoConfig: DynamoConfig): TransformableParser[Transformable] = {
    dynamoConfig.table match{
      case "MiroData" => new MiroParser
      case "CalmData" => new CalmParser
      case _ => throw new RuntimeException("Source not recognised")
    }
  }
}

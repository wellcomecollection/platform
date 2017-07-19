package uk.ac.wellcome.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.{CalmTransformable, MiroTransformable, Transformable}
import uk.ac.wellcome.transformer.parsers.{CalmParser, MiroParser, TransformableParser}

object TransformableParserModule extends TwitterModule {
  val dataSource =
    flag[String]("transformer.source", "", "Name of the source of data")

  @Singleton
  @Provides
  def providesTransformableParser(): TransformableParser[Transformable] = {

    dataSource() match {
      case "MiroData" => new MiroParser
      case "CalmData" => new CalmParser
      case tableName =>
        throw new RuntimeException(s"$tableName is not a recognised source")
    }
  }
}

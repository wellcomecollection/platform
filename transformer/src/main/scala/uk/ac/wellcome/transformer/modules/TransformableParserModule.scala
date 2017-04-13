package uk.ac.wellcome.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.Transformable
import uk.ac.wellcome.transformer.parsers.{MiroParser, TransformableParser}

object TransformableParserModule extends TwitterModule {
  private val source = flag[String]("source", "", "Name of the data source")

  @Singleton
  @Provides
  def providesTransformableParser: TransformableParser[Transformable] = {
    source() match{
      case "Miro" => new MiroParser
      case _ => throw new RuntimeException("Source not recognised")
    }
  }
}

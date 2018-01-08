package uk.ac.wellcome.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.transformer.transformers.{
  CalmTransformableTransformer,
  MiroTransformableTransformer,
  SierraTransformableTransformer,
  TransformableTransformer
}

object TransformerModule extends TwitterModule {
  val dataSource =
    flag[String]("transformer.source", "", "Name of the source of data")

  @Singleton
  @Provides
  def providesTransformableTransformer()
    : TransformableTransformer[Transformable] = {

    dataSource() match {
      case "MiroData" => new MiroTransformableTransformer
      case "CalmData" => new CalmTransformableTransformer
      case "SierraData" => new SierraTransformableTransformer
      case tableName =>
        throw new RuntimeException(s"$tableName is not a recognised source")
    }
  }
}

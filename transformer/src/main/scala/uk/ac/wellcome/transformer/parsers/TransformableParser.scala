package uk.ac.wellcome.transformer.parsers

import uk.ac.wellcome.models.Transformable
import uk.ac.wellcome.transformer.receive.RecordMap

import scala.util.Try

trait TransformableParser {
  def extractTransformable(recordMap: RecordMap): Try[Transformable]
}

package uk.ac.wellcome.platform.transformer.miro.exceptions

case class MiroTransformerException(e: Throwable)
    extends Exception(e.getMessage)

case object MiroTransformerException {
  def apply(message: String): MiroTransformerException =
    MiroTransformerException(new RuntimeException(message))
}

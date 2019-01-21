package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal.WorkType

trait MiroWorkType {

  /** We set the same work type on all Miro images.
    *
    * This is based on the Sierra work types -- we'll want to revisit this
    * when we sort out work types properly, but it'll do for now.
    */
  def getWorkType: Option[WorkType] =
    Some(
      WorkType(
        id = "q",
        label = "Digital Images"
      ))
}

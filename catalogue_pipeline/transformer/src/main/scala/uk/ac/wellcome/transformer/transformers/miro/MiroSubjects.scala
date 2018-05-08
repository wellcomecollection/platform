package uk.ac.wellcome.transformer.transformers.miro

import uk.ac.wellcome.models.work.internal.{AbstractConcept, Concept, Subject}
import uk.ac.wellcome.transformer.source.MiroTransformableData

trait MiroSubjects {

  /* Populate the subjects field.  This is based on two fields in the XML,
   *  <image_keywords> and <image_keywords_unauth>.  Both of these were
   *  defined in part or whole by the human cataloguers, and in general do
   *  not correspond to a controlled vocabulary.  (The latter was imported
   *  directly from PhotoSoft.)
   *
   *  In some cases, these actually do correspond to controlled vocabs,
   *  e.g. where keywords were pulled directly from Sierra -- but we don't
   *  have enough information in Miro to determine which ones those are.
   */
  def getSubjects(
    miroData: MiroTransformableData): List[Subject[AbstractConcept]] = {
    val keywords: List[Subject[AbstractConcept]] = miroData.keywords match {
      case Some(k) =>
        k.map { keyword =>
          Subject[AbstractConcept](
            label = keyword,
            concepts = List(Concept(keyword))
          )
        }
      case None =>
        List()
    }

    val keywordsUnauth: List[Subject[AbstractConcept]] =
      miroData.keywordsUnauth match {
        case Some(k) =>
          k.map { keyword =>
            Subject[AbstractConcept](
              label = keyword,
              concepts = List(Concept(keyword))
            )
          }
        case None =>
          List()
      }

    keywords ++ keywordsUnauth
  }
}

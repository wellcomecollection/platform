package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.models.work.text.TextNormalisation.sentenceCase

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
  def getSubjects(miroRecord: MiroRecord)
    : List[Unidentifiable[Subject[Unidentifiable[Concept]]]] = {
    val keywords: List[String] = miroRecord.keywords.getOrElse(List())

    val keywordsUnauth: List[String] =
      miroRecord.keywordsUnauth match {
        case Some(maybeKeywords) => maybeKeywords.flatten
        case None                => List()
      }

    (keywords ++ keywordsUnauth).map { keyword =>
      val normalisedLabel = sentenceCase(keyword)
      Unidentifiable(
        Subject[Unidentifiable[Concept]](
          label = normalisedLabel,
          concepts = List(Unidentifiable(Concept(normalisedLabel)))
        )
      )
    }
  }
}

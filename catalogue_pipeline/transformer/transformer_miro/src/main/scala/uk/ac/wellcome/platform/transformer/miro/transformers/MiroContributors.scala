package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.models.work.internal
import uk.ac.wellcome.models.work.internal.{Agent, Contributor, Unidentifiable}
import uk.ac.wellcome.platform.transformer.miro.exceptions.MiroTransformerException
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

trait MiroContributors extends MiroContributorCodes {
  /* Populate wwork:contributors.  We use the <image_creator> tag from the Miro XML. */
  def getContributors(
    miroRecord: MiroRecord): List[Contributor[Unidentifiable[Agent]]] = {
    val primaryCreators = miroRecord.creator match {
      case Some(maybeCreators) =>
        maybeCreators.collect {
          case Some(c) => Unidentifiable(Agent(c))
        }
      case None => List()
    }

    // <image_secondary_creator>: what MIRO calls Secondary Creator, which
    // will also just have to map to our object property "hasCreator"
    val secondaryCreators = miroRecord.secondaryCreator match {
      case Some(creator) =>
        creator.map { c =>
          Unidentifiable(Agent(c))
        }
      case None => List()
    }

    // We also add the contributor code for the non-historical images, but
    // only if the contributor *isn't* Wellcome Collection.v
    val maybeContributorCreator = miroRecord.sourceCode match {
      case Some(code) =>
        lookupContributorCode(miroId = miroRecord.imageNumber, code = code) match {
          case Some("Wellcome Collection") => None
          case Some(s)                     => Some(s)
          case None =>
            throw MiroTransformerException(
              s"Unable to look up contributor credit line for ${miroRecord.sourceCode} on ${miroRecord.imageNumber}"
            )
        }
      case None => None
    }

    val contributorCreators = maybeContributorCreator match {
      case Some(contributor) => List(Unidentifiable(Agent(contributor)))
      case None              => List()
    }

    val creators = primaryCreators ++ secondaryCreators ++ contributorCreators

    creators.map { agent: Unidentifiable[Agent] =>
      internal.Contributor(agent = agent)
    }
  }
}

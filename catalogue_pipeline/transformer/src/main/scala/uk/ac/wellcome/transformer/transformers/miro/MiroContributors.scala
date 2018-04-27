package uk.ac.wellcome.transformer.transformers.miro

import java.io.InputStream

import uk.ac.wellcome.models.work.internal
import uk.ac.wellcome.models.work.internal.{Agent, Contributor, Unidentifiable}
import uk.ac.wellcome.transformer.source.MiroTransformableData
import uk.ac.wellcome.utils.JsonUtil.toMap

import scala.io.Source

trait MiroContributors {

  // This JSON resource gives us credit lines for contributor codes.
  //
  // It is constructed as a map with fields drawn from the `contributors.xml`
  // export from Miro, with:
  //
  //     - `contributor_id` as the key
  //     - `contributor_credit_line` as the value
  //
  // Note that the checked-in file has had some manual edits for consistency,
  // and with a lot of the Wellcome-related strings replaced with
  // "Wellcome Collection".  There are also a handful of manual edits
  // where the fields in Miro weren't filled in correctly.
  val stream: InputStream = getClass
    .getResourceAsStream("/miro_contributor_map.json")
  val contributorMap =
    toMap[String](Source.fromInputStream(stream).mkString).get

  /* Populate wwork:contributors.  We use the <image_creator> tag from the Miro XML. */
  def getContributors(miroData: MiroTransformableData)
    : List[Contributor[Unidentifiable[Agent]]] = {
    val primaryCreators = miroData.creator match {
      case Some(maybeCreators) =>
        maybeCreators.collect {
          case Some(c) => Unidentifiable(Agent(c))
        }
      case None => List()
    }

    // <image_secondary_creator>: what MIRO calls Secondary Creator, which
    // will also just have to map to our object property "hasCreator"
    val secondaryCreators = miroData.secondaryCreator match {
      case Some(creator) =>
        creator.map { c =>
          Unidentifiable(Agent(c))
        }
      case None => List()
    }

    // We also add the contributor code for the non-historical images, but
    // only if the contributor *isn't* Wellcome Collection.v
    val contributorCreators = miroData.sourceCode match {
      case Some(code) =>
        contributorMap(code.toUpperCase) match {
          case "Wellcome Collection" => List()
          case contributor => List(Unidentifiable(Agent(contributor)))
        }
      case None => List()
    }

    val creators = primaryCreators ++ secondaryCreators ++ contributorCreators

    creators.map { agent: Unidentifiable[Agent] =>
      internal.Contributor(agent = agent)
    }
  }
}

package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.{AbstractAgent, Organisation, Person}
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraCreators extends MarcUtils {

  def getCreators(bibData: SierraBibData): List[AbstractAgent] = {

    val persons =
      getMatchingSubfields(bibData, "100", List("a", "b", "c")).map {
        subfields =>
          val name = subfields.collectFirst {
            case MarcSubfield("a", content) => content
          }
          val numeration = subfields.collectFirst {
            case MarcSubfield("b", content) => content
          }
          val prefixes = subfields.collect {
            case MarcSubfield("c", content) => content
          }

          val prefixString =
            if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))

          Person(
            label = name.get,
            prefix = prefixString,
            numeration = numeration)
      }

    val organisations = getMatchingSubfields(bibData, "110", "a").map {
      subfields =>
        Organisation(subfields.head.content)
    }

    persons ++ organisations
  }
}

package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraCreators extends MarcUtils {

  def getCreators(bibData: SierraBibData): List[IdentifieableOrUnidentifiable[AbstractAgent]] = {

    val persons =
      getMatchingSubfields(bibData, "100", List("a", "b", "c", "0")).map {
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
          val codes = subfields.collect {
            case MarcSubfield("0", content) => content
          }
          val prefixString =
            if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))

          val person = Person(
            label = name.get,
            prefix = prefixString,
            numeration = numeration)

          codes.distinct match{
            case Nil => Unidentifiable(person)
            case Seq(code) => Identifiable(person, SourceIdentifier(IdentifierSchemes.libraryOfCongressNames, code.trim))
            case _ => throw new RuntimeException(s"Multiple identifiers in subfield $$0: $codes")
          }
      }

    val organisations = getMatchingSubfields(bibData, "110", "a").map {
      subfields =>
        Unidentifiable(Organisation(subfields.head.content))
    }

    persons ++ organisations
  }
}

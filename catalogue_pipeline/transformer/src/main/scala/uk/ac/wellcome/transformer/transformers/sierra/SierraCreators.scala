package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraCreators extends MarcUtils {

  def getCreators(bibData: SierraBibData)
    : List[IdentifieableOrUnidentifiable[AbstractAgent]] = {

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

          identify(codes, person)
      }

    val organisations =
      getMatchingSubfields(bibData, "110", List("a", "0")).map { subfields =>
        val name = subfields.collectFirst {
          case MarcSubfield("a", content) => content
        }
        val codes = subfields.collect {
          case MarcSubfield("0", content) => content
        }
        val organisation = Organisation(name.get)
        identify(codes, organisation)
      }

    persons ++ organisations
  }

  private def identify(codes: List[String], agent: AbstractAgent) = {
    codes.distinct match {
      case Nil => Unidentifiable(agent)
      case Seq(code) =>
        val sourceIdentifier =
          SourceIdentifier(IdentifierSchemes.libraryOfCongressNames, code.trim)
        Identifiable(agent, sourceIdentifier, List(sourceIdentifier))
      case _ =>
        throw new RuntimeException(
          s"Multiple identifiers in subfield $$0: $codes")
    }
  }
}

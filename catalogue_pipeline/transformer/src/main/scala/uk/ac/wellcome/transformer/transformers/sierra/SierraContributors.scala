package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraContributors extends MarcUtils {

  /* Populate wwork:contributors. Rules:
   *
   * For bib records with MARC tag 100 or 700, create a "Person" with:
   *  - Subfield $b as "numeration" (NR)
   *  - Subfield $c as "prefix", joined with spaces into a single string (R)
   *
   * TODO: Check this is the correct way to construct the prefix.
   *
   * For bib records with MARC tag 110 or 710, create an "Organisation".
   *
   * For all entries:
   *  - Subfield $a is "label"
   *  - Subfield $0 is used to populate "identifiers".  The identifier scheme
   *    is lc-names.
   *    TODO: Do we need to clean up these strings, e.g. remove spaces?
   *  - Subfield $e is used for the labels in "roles"
   *
   * Order by MARC tag (100, 110, 700, 710), then by order of appearance
   * in the MARC data.
   *
   * https://www.loc.gov/marc/bibliographic/bd100.html
   * https://www.loc.gov/marc/bibliographic/bd110.html
   * https://www.loc.gov/marc/bibliographic/bd700.html
   * https://www.loc.gov/marc/bibliographic/bd710.html
   *
   */
  def getContributors(
    bibData: SierraBibData): List[Contributor[MaybeDisplayable[AbstractAgent]]] = {
    getPersons(bibData, marcTag = "100") ++ getPersons(bibData, marcTag = "700")
  }

  /* For a given MARC tag (100 or 700), return a list of all the Contributor[Person] instances
   * this MARC tag represents.
   */
  private def getPersons(bibData: SierraBibData, marcTag: String): List[Contributor[MaybeDisplayable[Person]]] = {
    val persons = getMatchingSubfields(
      bibData,
      marcTag = marcTag,
      marcSubfieldTags = List("a", "c")
    )

    persons.map { subfields =>

      // Extract the label from subfield $a
      val label = subfields.collectFirst {
        case MarcSubfield("a", content) => content
      }.get

      // Extract the prefix from subfield $c
      val prefixes = subfields.collect {
        case MarcSubfield("c", content) => content
      }
      val prefixString = if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))

      Contributor[MaybeDisplayable[Person]](
        agent = Unidentifiable(
          Person(label = label, prefix = prefixString)
        )
      )
    }
  }
}

package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData}

trait SierraContributors extends MarcUtils {

  /* Populate wwork:contributors. Rules:
   *
   * For bib records with MARC tag 100 or 700, create a "Person" with:
   *  - Subfield $b as "numeration"
   *  - Subfield $c as "prefix", joined with spaces into a single string
   *
   * TODO: Check this is the correct way to construct the prefix.
   *
   * For bib records with MARC tag 110 or 710, create an "Organisation".
   *
   * For all entries:
   *  - Subfield $a is "label"
   *  - Subfield $0 is used to populate "identifiers".  The identifier scheme
   *    is lc-names.
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
  def getContributors(bibData: SierraBibData)
    : List[Contributor[MaybeDisplayable[AbstractAgent]]] = {
    getPersons(bibData, marcTag = "100") ++
      getOrganisations(bibData, marcTag = "110") ++
      getPersons(bibData, marcTag = "700") ++
      getOrganisations(bibData, marcTag = "710")
  }

  /* For a given MARC tag (100 or 700), return a list of all the Contributor[Person] instances
   * this MARC tag represents.
   */
  private def getPersons(
    bibData: SierraBibData,
    marcTag: String): List[Contributor[MaybeDisplayable[Person]]] = {
    val persons = getMatchingSubfields(
      bibData,
      marcTag = marcTag,
      marcSubfieldTags = List("a", "b", "c", "e", "0")
    )

    persons.map { subfields =>
      val label = getLabel(subfields)
      val roles = getContributionRoles(subfields)

      // Extract the numeration from subfield $b.  This is also non-repeatable
      // in the MARC spec.
      val numeration = subfields.collectFirst {
        case MarcSubfield("b", content) => content
      }

      // Extract the prefix from subfield $c.  This is a repeatable field, so
      // we take all instances and join them.
      val prefixes = subfields.collect {
        case MarcSubfield("c", content) => content
      }
      val prefixString =
        if (prefixes.isEmpty) None else Some(prefixes.mkString(" "))

      val agent = identify[Person](
        subfields = subfields,
        agent = Person(
          label = label,
          prefix = prefixString,
          numeration = numeration
        ),
        ontologyType = "Person"
      )

      internal.Contributor[MaybeDisplayable[Person]](
        agent = agent,
        roles = roles
      )
    }
  }

  /* For a given MARC tag (110 or 710), return a list of all the Contributor[Organisation] instances
   * this MARC tag represents.
   */
  private def getOrganisations(
    bibData: SierraBibData,
    marcTag: String): List[Contributor[MaybeDisplayable[Organisation]]] = {
    val organisations = getMatchingSubfields(
      bibData,
      marcTag = marcTag,
      marcSubfieldTags = List("a", "b", "c", "e", "0")
    )

    organisations.map { subfields =>
      val label = getLabel(subfields)
      val roles = getContributionRoles(subfields)

      val agent = identify[Organisation](
        subfields = subfields,
        agent = Organisation(label = label),
        ontologyType = "Organisation"
      )

      internal.Contributor[MaybeDisplayable[Organisation]](
        agent = agent,
        roles = roles
      )
    }
  }

  private def getLabel(subfields: List[MarcSubfield]): String = {
    // Extract the label from subfield $a.  This is a non-repeatable
    // field in the MARC spec, so collectFirst is okay.
    subfields.collectFirst {
      case MarcSubfield("a", content) => content
    }.get
  }

  private def getContributionRoles(
    subfields: List[MarcSubfield]): List[ContributionRole] = {
    // Extract the roles from subfield $e.  This is a repeatable field.
    subfields.collect {
      case MarcSubfield("e", content) => ContributionRole(content)
    }
  }

  /* Given an agent and the associated MARC subfields, look for instances of subfield $0,
   * which are used for identifiers.
   *
   * This methods them (if present) and wraps the agent in Unidentifiable or Identifiable
   * as appropriate.
   */
  private def identify[T](subfields: List[MarcSubfield],
                          agent: T,
                          ontologyType: String): MaybeDisplayable[T] = {

    // We take the contents of subfield $0.  They may contain inconsistent
    // spacing and punctuation, such as:
    //
    //    " nr 82270463"
    //    "nr 82270463"
    //    "nr 82270463.,"
    //
    // which all refer to the same identifier.
    //
    // For consistency, we remove all whitespace and some punctuation
    // before continuing.
    val codes = subfields.collect {
      case MarcSubfield("0", content) => content.replaceAll("[.,\\s]", "")
    }

    codes.distinct match {
      case Nil => Unidentifiable(agent)
      case Seq(code) => {
        val sourceIdentifier = SourceIdentifier(
          identifierScheme = IdentifierSchemes.libraryOfCongressNames,
          value = code,
          ontologyType = ontologyType
        )
        Identifiable(
          agent = agent,
          sourceIdentifier = sourceIdentifier,
          identifiers = List(sourceIdentifier)
        )
      }
      case _ =>
        throw new RuntimeException(
          s"Multiple identifiers in subfield $$0: $codes")
    }
  }
}

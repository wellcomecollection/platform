package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData}

trait SierraContributors extends MarcUtils with SierraAgents {

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
    getPersonContributors(bibData, marcTag = "100") ++
      getOrganisationContributors(bibData, marcTag = "110") ++
      getPersonContributors(bibData, marcTag = "700") ++
      getOrganisationContributors(bibData, marcTag = "710")
  }

  /* For a given MARC tag (100 or 700), return a list of all the Contributor[Person] instances
   * this MARC tag represents.
   */
  private def getPersonContributors(
    bibData: SierraBibData,
    marcTag: String): List[Contributor[MaybeDisplayable[Person]]] = {
    val persons = getMatchingSubfields(
      bibData,
      marcTag = marcTag,
      marcSubfieldTags = List("a", "b", "c", "e", "0")
    )

    persons.map { subfields =>
      val roles = getContributionRoles(subfields)
      val agent = getPerson(subfields)

      Contributor(
        agent = identify(subfields, agent, "Person"),
        roles = roles
      )
    }
  }

  /* For a given MARC tag (110 or 710), return a list of all the Contributor[Organisation] instances
   * this MARC tag represents.
   */
  private def getOrganisationContributors(
    bibData: SierraBibData,
    marcTag: String): List[Contributor[MaybeDisplayable[Organisation]]] = {
    val organisations = getMatchingSubfields(
      bibData,
      marcTag = marcTag,
      marcSubfieldTags = List("a", "b", "c", "e", "0")
    )

    organisations.map { subfields =>
      val roles = getContributionRoles(subfields)
      val agent = getOrganisation(subfields)

      Contributor(
        agent = identify(subfields, agent, "Organisation"),
        roles = roles
      )
    }
  }

  private def getContributionRoles(
    subfields: List[MarcSubfield]): List[ContributionRole] = {
    // Extract the roles from subfield $e.  This is a repeatable field.
    subfields.collect {
      case MarcSubfield("e", content) => ContributionRole(content)
    }
  }
}

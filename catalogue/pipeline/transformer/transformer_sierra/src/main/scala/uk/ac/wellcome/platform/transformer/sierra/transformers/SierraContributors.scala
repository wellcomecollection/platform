package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData
}

trait SierraContributors extends MarcUtils with SierraAgents {

  /* Populate wwork:contributors. Rules:
   *
   * For bib records with MARC tag 100 or 700, create a "Person":
   *
   * For bib records with MARC tag 110 or 710, create an "Organisation".
   *
   * For Persons and Organisations, subfield $e is used for the labels in "roles"
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

    persons
      .flatMap { subfields: List[MarcSubfield] =>
        val roles = getContributionRoles(subfields)
        val maybePerson = getPerson(subfields)

        maybePerson.map { person =>
          Contributor(
            agent = identify(subfields, person, "Person"),
            roles = roles
          )
        }
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

    organisations
      .flatMap { subfields: List[MarcSubfield] =>
        val roles = getContributionRoles(subfields)
        val maybeAgent = getOrganisation(subfields)

        maybeAgent.map { agent =>
          Contributor(
            agent = identify(subfields, agent, "Organisation"),
            roles = roles
          )
        }
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

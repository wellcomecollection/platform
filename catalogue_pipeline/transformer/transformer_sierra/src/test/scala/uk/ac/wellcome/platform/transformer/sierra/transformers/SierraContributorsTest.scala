package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraContributorsTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  val transformer = new SierraContributors {}

  it("gets an empty contributor list from empty bib data") {
    transformAndCheckContributors(
      varFields = List(),
      expectedContributors = List())
  }

  it("extracts a mixture of Person and Organisation contributors") {
    val varFields = List(
      createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sarah the soybean")
        )
      ),
      createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sam the squash"),
          MarcSubfield(tag = "c", content = "Sir")
        )
      ),
      createVarFieldWith(
        marcTag = "110",
        subfields = List(
          MarcSubfield(tag = "a", content = "Spinach Solicitors")
        )
      ),
      createVarFieldWith(
        marcTag = "700",
        subfields = List(
          MarcSubfield(tag = "a", content = "Sebastian the sugarsnap")
        )
      ),
      createVarFieldWith(
        marcTag = "710",
        subfields = List(
          MarcSubfield(tag = "a", content = "Shallot Swimmers")
        )
      )
    )

    val expectedContributors = List(
      Contributor(agent = Unidentifiable(Person("Sarah the soybean"))),
      Contributor(
        agent = Unidentifiable(Person("Sam the squash", Some("Sir")))),
      Contributor(agent = Unidentifiable(Organisation("Spinach Solicitors"))),
      Contributor(agent = Unidentifiable(Person("Sebastian the sugarsnap"))),
      Contributor(agent = Unidentifiable(Organisation("Shallot Swimmers")))
    )
    transformAndCheckContributors(
      varFields = varFields,
      expectedContributors = expectedContributors)
  }

  describe("Person") {
    it("gets the name from MARC tag 100 subfield $$a") {
      val name = "Carol the Carrot"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(MarcSubfield(tag = "a", content = name))
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Person(label = name)))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the name from MARC tag 700 subfield $$a") {
      val name = "Bertrand the Beetroot"

      val varFields = List(
        createVarFieldWith(
          marcTag = "700",
          subfields = List(MarcSubfield(tag = "a", content = name))
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Person(label = name)))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "gets the name from MARC tags 100 and 700 subfield $$a in the right order") {
      val name1 = "Alfie the Artichoke"
      val name2 = "Alison the Apple"
      val name3 = "Archie the Aubergine"

      // The correct ordering is "everything from 100 first, then 700", and
      // we deliberately pick an ordering that's different from that for
      // the MARC fields, so we can check it really is applying this rule.
      val varFields = List(
        createVarFieldWith(
          marcTag = "700",
          subfields = List(MarcSubfield(tag = "a", content = name2))
        ),
        createVarFieldWith(
          marcTag = "100",
          subfields = List(MarcSubfield(tag = "a", content = name1))
        ),
        createVarFieldWith(
          marcTag = "700",
          subfields = List(MarcSubfield(tag = "a", content = name3))
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Person(label = name1))),
        Contributor(agent = Unidentifiable(Person(label = name2))),
        Contributor(agent = Unidentifiable(Person(label = name3)))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the creator prefix from MARC tag 100 subfield $$c") {
      val name = "Darla the Dandelion"
      val prefix = "Dr"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "c", content = prefix)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(
            Person(
              label = name,
              prefix = Some(prefix)
            ))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the creator prefix from MARC tag 700 subfield $$c") {
      val name = "Roland the Radish"
      val prefix = "Rev"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "c", content = prefix)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(
            Person(
              label = name,
              prefix = Some(prefix)
            ))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("joins multiple instances of subfield $$c into a single prefix") {
      val name = "Mick the Mallow"
      val prefix1 = "Mx"
      val prefix2 = "Mr"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "c", content = prefix1),
            MarcSubfield(tag = "c", content = prefix2)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(
            Person(
              label = name,
              prefix = Some(s"${prefix1} ${prefix2}")
            ))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the numeration from subfield $$b") {
      val name = "Leopold the Lettuce"
      val numeration = "LX"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "b", content = numeration)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(
            Person(label = name, numeration = Some(numeration))
          ))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the roles from subfield $$e") {
      val name = "Violet the Vanilla"
      val role1 = "spice"
      val role2 = "flavour"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "e", content = role1),
            MarcSubfield(tag = "e", content = role2)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(Person(label = name)),
          roles = List(ContributionRole(role1), ContributionRole(role2))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets an identifier from subfield $$0") {
      val name = "Ivan the ivy"
      val lcshCode = "lcsh7101607"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Person",
        value = lcshCode
      )

      val expectedContributors = List(
        Contributor(
          agent = Identifiable(
            Person(label = name),
            sourceIdentifier = sourceIdentifier
          ))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "combines identifiers with inconsistent spacing/punctuation from subfield $$0") {
      val name = "Wanda the watercress"
      val lcshCodeCanonical = "lcsh2055034"
      val lcshCode1 = "lcsh 2055034"
      val lcshCode2 = "  lcsh2055034 "
      val lcshCode3 = " lc sh 2055034"

      // Based on an example from a real record; see Sierra b3017492.
      val lcshCode4 = "lcsh 2055034.,"

      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode1),
            MarcSubfield(tag = "0", content = lcshCode2),
            MarcSubfield(tag = "0", content = lcshCode3),
            MarcSubfield(tag = "0", content = lcshCode4)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Person",
        value = lcshCodeCanonical
      )

      val expectedContributors = List(
        Contributor(
          agent = Identifiable(
            Person(label = name),
            sourceIdentifier = sourceIdentifier
          ))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "does not identify the contributor if there are multiple distinct identifiers in subfield $$0") {
      val name = "Darren the Dill"
      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = "lcsh9069541"),
            MarcSubfield(tag = "0", content = "lcsh3384149")
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(Person(label = name))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("normalises Person contributor labels") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "100",
          subfields = List(MarcSubfield(tag = "a", content = "George,"))
        ),
        createVarFieldWith(
          marcTag = "700",
          subfields = List(
            MarcSubfield(tag = "a", content = "Sebastian,")
          )
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Person(label = "George"))),
        Contributor(agent = Unidentifiable(Person(label = "Sebastian")))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }
  }

  describe("Organisation") {
    it("gets the name from MARC tag 110 subfield $$a") {
      val name = "Ona the orache"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(MarcSubfield(tag = "a", content = name))
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Organisation(label = name)))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the name from MARC tag 710 subfield $$a") {
      val name = "Karl the kale"

      val varFields = List(
        createVarFieldWith(
          marcTag = "710",
          subfields = List(MarcSubfield(tag = "a", content = name))
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Organisation(label = name)))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "gets the name from MARC tags 110 and 710 subfield $$a in the right order") {
      val name1 = "Mary the mallow"
      val name2 = "Mike the mashua"
      val name3 = "Mickey the mozuku"

      // The correct ordering is "everything from 110 first, then 710", and
      // we deliberately pick an ordering that's different from that for
      // the MARC fields, so we can check it really is applying this rule.
      val varFields = List(
        createVarFieldWith(
          marcTag = "710",
          subfields = List(MarcSubfield(tag = "a", content = name2))
        ),
        createVarFieldWith(
          marcTag = "110",
          subfields = List(MarcSubfield(tag = "a", content = name1))
        ),
        createVarFieldWith(
          marcTag = "710",
          subfields = List(MarcSubfield(tag = "a", content = name3))
        )
      )

      val expectedContributors = List(
        Contributor(agent = Unidentifiable(Organisation(label = name1))),
        Contributor(agent = Unidentifiable(Organisation(label = name2))),
        Contributor(agent = Unidentifiable(Organisation(label = name3)))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets the roles from subfield $$e") {
      val name = "Terry the turmeric"
      val role1 = "dye"
      val role2 = "colouring"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "e", content = role1),
            MarcSubfield(tag = "e", content = role2)
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(Organisation(label = name)),
          roles = List(ContributionRole(role1), ContributionRole(role2))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets an identifier from subfield $$0") {
      val name = "Gerry the Garlic"
      val lcshCode = "lcsh7212"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Organisation",
        value = lcshCode
      )

      val expectedContributors = List(
        Contributor(
          agent = Identifiable(
            Organisation(label = name),
            sourceIdentifier = sourceIdentifier
          ))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("gets an identifier with inconsistent spacing from subfield $$0") {
      val name = "Charlie the chive"
      val lcshCodeCanonical = "lcsh6791210"
      val lcshCode1 = "lcsh 6791210"
      val lcshCode2 = "  lcsh6791210 "
      val lcshCode3 = " lc sh 6791210"

      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = lcshCode1),
            MarcSubfield(tag = "0", content = lcshCode2),
            MarcSubfield(tag = "0", content = lcshCode3)
          )
        )
      )

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("lc-names"),
        ontologyType = "Organisation",
        value = lcshCodeCanonical
      )

      val expectedContributors = List(
        Contributor(
          agent = Identifiable(
            Organisation(label = name),
            sourceIdentifier = sourceIdentifier
          ))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it(
      "does not identify the contributor if there are multiple distinct identifiers in subfield $$0") {
      val name = "Luke the lime"
      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = "lcsh3349285"),
            MarcSubfield(tag = "0", content = "lcsh9059917")
          )
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(Organisation(label = name))
        )
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }

    it("normalises Organisation contributor labels") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "110",
          subfields =
            List(MarcSubfield(tag = "a", content = "The organisation,"))
        ),
        createVarFieldWith(
          marcTag = "710",
          subfields =
            List(MarcSubfield(tag = "a", content = "Another organisation,"))
        )
      )

      val expectedContributors = List(
        Contributor(
          agent = Unidentifiable(Organisation(label = "The organisation"))),
        Contributor(
          agent = Unidentifiable(Organisation(label = "Another organisation")))
      )

      transformAndCheckContributors(
        varFields = varFields,
        expectedContributors = expectedContributors)
    }
  }

  // This is based on transformer failures we saw in October 2018 --
  // records 3069865, 3069866, 3069867, 3069872 all had empty instances of
  // the 110 field.
  it("returns an empty list if subfield $$a is missing") {
    val varFields = List(
      createVarFieldWith(
        marcTag = "100",
        subfields = List(
          MarcSubfield(tag = "e", content = "")
        )
      )
    )

    transformAndCheckContributors(
      varFields = varFields,
      expectedContributors = List())
  }

  private def transformAndCheckContributors(
    varFields: List[VarField],
    expectedContributors: List[Contributor[MaybeDisplayable[AbstractAgent]]]
  ) = {
    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getContributors(bibData) shouldBe expectedContributors
  }
}

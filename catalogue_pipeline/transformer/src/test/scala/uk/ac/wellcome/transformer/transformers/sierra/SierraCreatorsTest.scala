package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

class SierraCreatorsTest extends FunSpec with Matchers {

  val transformer = new SierraCreators {}

  it("extracts the creator name from marcTag 100 a") {
    val name = "Carrot Ironfoundersson"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(MarcSubfield(tag = "a", content = name))))
    )

    val creators = transformer.getCreators(bibData)

    creators should contain only Unidentifiable(Person(label = name))
  }

  it("extracts the creator numeration and prefix if present from marcTag 100") {
    val name = "Havelock Vetinari"
    val prefix = "Lord Patrician"
    val numeration = "I"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "b", content = numeration),
            MarcSubfield(tag = "c", content = prefix))
        ))
    )

    val creators = transformer.getCreators(bibData)

    creators should contain only Unidentifiable(
      Person(
        label = name,
        prefix = Some(prefix),
        numeration = Some(numeration)))
  }

  it("extracts multiple prefixes from marcTag 100 c") {
    val name = "Samuel Vines"
    val prefixes = List("Commander", "His Grace, The Duke of Ankh")

    val prefixSubfields =
      prefixes.map(prefix => MarcSubfield(tag = "c", content = prefix))

    val subfields = prefixSubfields :+ MarcSubfield(tag = "a", content = name)
    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = subfields))
    )

    val creators = transformer.getCreators(bibData)

    creators should contain only Unidentifiable(
      Person(label = name, prefix = Some(prefixes.mkString(" "))))
  }

  it("extracts the creator identifier from marcTag 100 0") {
    val name = "Carrot Ironfoundersson"
    val code = "123456"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code))))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Person",
      code)
    creators should contain only Identifiable(
      Person(label = name),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier))
  }

  it(
    "extracts the creator identifier removing trailing and leading from marcTag 100 0") {
    val name = "Carrot Ironfoundersson"
    val code = " 123456 "

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code))))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Person",
      code.trim)
    creators should contain only Identifiable(
      Person(label = name),
      sourceIdentifier = sourceIdentifier,
      List(sourceIdentifier))
  }

  // TODO: find out the identifiers normalisation rules
  // This is ignored until we get better information as to
  // how we want to clean the identifiers data
  ignore(
    "extracts the creator identifier removing trailing and leading from marcTag 100 0") {
    val name = "The Luggage"
    val code = "n 123456"
    val cleanedCode = "123456"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code))))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Person",
      code.trim)
    creators should contain only Identifiable(
      Person(label = name),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier))
  }

  it(
    "extracts the creator identifier from marcTag 100 0 if there is more than one but they are the same") {
    val name = "Greebo"
    val code = "123456"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code),
            MarcSubfield(tag = "0", content = code))
        ))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Person",
      code)
    creators should contain only Identifiable(
      Person(label = name),
      sourceIdentifier = sourceIdentifier,
      List(sourceIdentifier))
  }

  it(
    "fails extracting the creator identifier from marcTag 100 0 if there is more than one and they are different") {
    val name = "Errol"
    val firstCode = "123456"
    val secondCode = "654321"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "100",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = firstCode),
            MarcSubfield(tag = "0", content = secondCode))
        ))
    )

    intercept[RuntimeException] {
      transformer.getCreators(bibData)
    }
  }

  it("extracts organisations from marcTag 110") {
    val name = "Ankh-Morpork City Watch"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "110",
          indicator1 = "",
          indicator2 = "",
          subfields = List(MarcSubfield(tag = "a", content = name))))
    )

    val creators = transformer.getCreators(bibData)

    creators should contain only Unidentifiable(Organisation(label = name))
  }

  it("extracts the creator identifier from marcTag 110 0") {
    val name = "The Unseen University"
    val code = "123456"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "110",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code))))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Organisation",
      code)
    creators should contain only Identifiable(
      Organisation(label = name),
      sourceIdentifier = sourceIdentifier,
      List(sourceIdentifier))
  }

  it(
    "extracts the creator identifier removing trailing and leading from marcTag 110 0") {
    val name = "The Assassins' Guild"
    val code = " 123456 "

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "110",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code))))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Organisation",
      code.trim)
    creators should contain only Identifiable(
      Organisation(label = name),
      sourceIdentifier = sourceIdentifier,
      List(sourceIdentifier))
  }

  it(
    "extracts the creator identifier from marcTag 110 0 if there is more than one but they are the same") {
    val name = "The Fools' Guild"
    val code = "123456"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "110",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = code),
            MarcSubfield(tag = "0", content = code))
        ))
    )

    val creators = transformer.getCreators(bibData)

    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
      "Organisation",
      code)
    creators should contain only Identifiable(
      Organisation(label = name),
      sourceIdentifier = sourceIdentifier,
      List(sourceIdentifier))
  }

  it(
    "fails extracting the creator identifier from marcTag 110 0 if there is more than one and they are different") {
    val name = "The Fools' Guild"
    val firstCode = "123456"
    val secondCode = "654321"

    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "110",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = name),
            MarcSubfield(tag = "0", content = firstCode),
            MarcSubfield(tag = "0", content = secondCode))
        ))
    )

    intercept[RuntimeException] {
      transformer.getCreators(bibData)
    }
  }

}

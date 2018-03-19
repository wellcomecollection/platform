package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{AbstractAgent, Organisation, Person}
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

    creators should contain only Person(label = name)
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

    creators should contain only Person(
      label = name,
      prefix = Some(prefix),
      numeration = Some(numeration))
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

    creators should contain only Person(
      label = name,
      prefix = Some(prefixes.mkString(" ")))
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

    creators should contain only Organisation(label = name)
  }

}

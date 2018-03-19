package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{AbstractAgent, Person}
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraCreatorsTest extends FunSpec with Matchers {

  it("extracts the creator name from marcTag 100 a"){
    val name = "Samuel Vines"

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

    val transformer = new SierraCreators {}

    val creators = transformer.getCreators(bibData)

    creators should contain only Person(name = name)
  }

  it("extracts the creator numeration and prefix if present from marcTag 100"){
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
          subfields = List(MarcSubfield(tag = "a", content = name),MarcSubfield(tag = "b", content = numeration),MarcSubfield(tag = "c", content = prefix))))
    )

    val transformer = new SierraCreators {}

    val creators = transformer.getCreators(bibData)

    creators should contain only Person(name = name, prefix = Some(prefix), numeration = Some(numeration))
  }

}

trait SierraCreators extends MarcUtils {
  def getCreators(bibData: SierraBibData): List[AbstractAgent] = getMatchingSubfields(bibData, "100", List("a", "b", "c")).map{subfields =>
    val name = subfields.collectFirst{ case MarcSubfield("a", content) => content}
    val numeration = subfields.collectFirst { case MarcSubfield("b", content) => content }
    val prefix = subfields.collectFirst { case MarcSubfield("c", content) => content }
    Person(name = name.get, prefix = prefix, numeration = numeration)
  }
}

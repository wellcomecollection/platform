package uk.ac.wellcome.platform.transformer.sierra.generators

import uk.ac.wellcome.platform.transformer.sierra.source.{MarcSubfield, VarField}

trait MarcGenerators {
  def createVarFieldWith(
    marcTag: String = "XXX",
    indicator2: Option[String] = None,
    subfields: List[MarcSubfield] = List()): VarField =
    VarField(
      fieldTag = "p",
      marcTag = Some(marcTag),
      indicator1 = None,
      indicator2 = indicator2,
      subfields = subfields
    )
}

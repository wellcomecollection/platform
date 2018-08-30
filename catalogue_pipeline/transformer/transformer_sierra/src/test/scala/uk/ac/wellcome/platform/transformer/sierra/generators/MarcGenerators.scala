package uk.ac.wellcome.platform.transformer.sierra.generators

import uk.ac.wellcome.platform.transformer.sierra.source.VarField

trait MarcGenerators {
  def createVarFieldWith(
    marcTag: String,
    indicator2: Option[String] = None): VarField =
    VarField(
      fieldTag = "p",
      marcTag = Some(marcTag),
      indicator1 = None,
      indicator2 = indicator2,
      subfields = List()
    )
}

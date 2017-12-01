package uk.ac.wellcome.models

case class MergedSierraObject(id: String, bibData: Option[String] = None)

object MergedSierraObject {
  def apply(id: String, bibData: String): MergedSierraObject =
    MergedSierraObject(id = id, bibData = Some(bibData))
}

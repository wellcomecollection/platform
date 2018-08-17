package uk.ac.wellcome.models.matcher

case class WorkIdentifier(identifier: String, version: Int) extends Versioned

case object WorkIdentifier {
  def apply(work: WorkNode): WorkIdentifier =
    WorkIdentifier(work.id, work.version)
}

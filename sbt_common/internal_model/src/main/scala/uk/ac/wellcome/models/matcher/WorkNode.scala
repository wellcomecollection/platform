package uk.ac.wellcome.models.matcher

case class WorkNode(workId: String, version: Int, linkedIds: List[String], setId: String)
  extends Versioned with IdentifiableWork

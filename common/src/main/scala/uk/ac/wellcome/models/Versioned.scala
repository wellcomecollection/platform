package uk.ac.wellcome.models

trait Versioned {
  val version: Int
  val sourceId: String
  val sourceName: String
}

trait VersionUpdater[T <: Versioned] {
  def updateVersion(versioned: T, newVersion: Int): T
}

package uk.ac.wellcome.models

trait Versioned {
  val version: Int
}

trait VersionUpdater[T] {
  def updateVersion(versioned: T, newVersion: Int): T
}

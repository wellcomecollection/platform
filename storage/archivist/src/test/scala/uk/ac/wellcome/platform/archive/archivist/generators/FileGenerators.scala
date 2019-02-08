package uk.ac.wellcome.platform.archive.archivist.generators

import uk.ac.wellcome.platform.archive.common.fixtures.{FileEntry, RandomThings}

trait FileGenerators extends RandomThings {
  def createFileEntry(filename: String): FileEntry = {
    FileEntry(name = filename, contents = randomAlphanumeric())
  }
}

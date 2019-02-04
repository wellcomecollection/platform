package uk.ac.wellcome.platform.archive.common.models

/** Represents the complete location of a Bag in S3.
  *
  * @param storageNamespace The name of the S3 bucket
  * @param storagePrefix The global prefix for all objects created by the archivist
  *                      (e.g. "archive")
  * @param storageSpace The namespace from the ingest request (e.g. "digitised")
  * @param bagPath The relative path to the bag (e.g. "b12345.zip")
  */
case class BagLocation(
  storageNamespace: String,
  storagePrefix: String,
  storageSpace: StorageSpace,
  bagPath: BagPath
) {
  def completeFilepath = s"$storagePrefix/$storageSpace/$bagPath"
}

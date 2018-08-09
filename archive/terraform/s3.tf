data "aws_s3_bucket" "storage_manifests" {
  bucket = "${var.storage_manifest_bucket}"
}

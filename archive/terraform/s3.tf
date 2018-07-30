data "aws_s3_bucket" "storage_manifests" {
  name = "${var.storage_manifest_bucket}"
}

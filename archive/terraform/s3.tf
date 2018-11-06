data "aws_s3_bucket" "storage_manifests" {
  bucket = "${module.vhs_archive_manifest.bucket_name}"
}

resource "aws_s3_bucket" "archive_storage" {
  bucket = "${local.archive_bucket_name}"
  acl    = "private"
}

resource "aws_s3_bucket" "ingest_storage" {
  bucket = "${local.ingest_bucket_name}"
  acl    = "private"
}

resource "aws_s3_bucket" "storage_static_content" {
  bucket = "${local.storage_static_content_bucket_name}"
  acl    = "private"
}

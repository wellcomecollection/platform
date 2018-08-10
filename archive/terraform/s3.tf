data "aws_s3_bucket" "storage_manifests" {
  bucket = "${module.vhs_archive_manifest.bucket_name}"
}

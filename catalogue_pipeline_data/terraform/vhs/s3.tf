resource "aws_s3_bucket" "bucket" {
  count = "${var.prevent_destroy == "false" ? 1 : 0}"
  bucket = "${local.bucket_name}"

  lifecycle {
    prevent_destroy = false
  }
}

resource "aws_s3_bucket" "transient_bucket" {
  count = "${var.prevent_destroy == "true" ? 1 : 0}"
  force_destroy = true

  bucket = "${local.bucket_name}"
}

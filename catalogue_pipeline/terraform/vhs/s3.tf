resource "aws_s3_bucket" "bucket" {
  bucket = "${var.bucket_prefix}${var.bucket_name}"

  lifecycle {
    prevent_destroy = true
  }
}

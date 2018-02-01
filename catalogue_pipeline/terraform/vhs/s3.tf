resource "aws_s3_bucket" "sierra_data" {
  bucket = "${var.bucket_name}"

  lifecycle {
    prevent_destroy = true
  }
}

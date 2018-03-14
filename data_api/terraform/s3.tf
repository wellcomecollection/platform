resource "aws_s3_bucket" "public_data" {
  bucket = "wellcomecollection-data-public"
  acl    = "public-read"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "private_data" {
  bucket = "wellcomecollection-data-private"

  lifecycle {
    prevent_destroy = true
  }
}
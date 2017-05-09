resource "aws_s3_bucket" "miro-data" {
  bucket = "miro-data"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "miro-images-sync" {
  bucket = "miro-images-sync"
  acl    = "private"
}

resource "aws_s3_bucket" "infra" {
  bucket = "${var.infra_bucket}"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

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

resource "aws_s3_bucket" "miro_images_public" {
  bucket = "miro-images-public"
  acl    = "public-read"
}

resource "aws_s3_bucket" "infra" {
  bucket = "${var.infra_bucket}"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "dashboard" {
  bucket = "${var.dash_bucket}"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}


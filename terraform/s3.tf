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
  acl    = "public-read"

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "mets-ingest" {
  bucket = "mets-ingest"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

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

  lifecycle_rule {
    id      = "move_to_infrequent_access"
    enabled = true
    prefix  = ""

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
  }
}

# This bucket needs to be removed when we transition to using wellcomecollection-miro-images-public
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

  lifecycle_rule {
    id      = "tmp"
    prefix  = "tmp/"
    enabled = true

    expiration {
      days = 30
    }
  }

  lifecycle_rule {
    id      = "elasticdump"
    prefix  = "elasticdump/"
    enabled = true

    expiration {
      days = 30
    }
  }
}

resource "aws_s3_bucket" "mets-ingest" {
  bucket = "mets-ingest"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "alb-logs" {
  bucket = "wellcomecollection-alb-logs"
  acl    = "private"

  policy = "${data.aws_iam_policy_document.alb_logs.json}"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "expire_alb_logs"
    enabled = true

    expiration {
      days = 30
    }
  }
}

resource "aws_s3_bucket" "wellcomecollection-mets-ingest" {
  bucket = "wellcomecollection-mets-ingest"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "cloudfront-logs" {
  bucket = "wellcome-platform-cloudfront-logs"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

/* Used for transferring files to/from third-parties */
resource "aws_s3_bucket" "transfer" {
  bucket = "wellcomecollection-transfer"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

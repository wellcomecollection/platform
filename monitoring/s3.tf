resource "aws_s3_bucket" "monitoring" {
  bucket = "wellcomecollection-platform-monitoring"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id      = "budget_graphs"
    prefix  = "budget_graphs/"
    enabled = true

    expiration {
      days = 30
    }
  }

  lifecycle_rule {
    id      = "gatling"
    prefix  = "gatling/"
    enabled = true

    expiration {
      days = 30
    }
  }

  lifecycle_rule {
    id      = "terraform_plans"
    prefix  = "terraform_plans/"
    enabled = true

    expiration {
      days = 30
    }
  }
}

resource "aws_s3_bucket" "dashboard" {
  bucket = "wellcomecollection-platform-dashboard"
  acl    = "public-read"

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

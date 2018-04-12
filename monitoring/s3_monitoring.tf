resource "aws_s3_bucket" "monitoring" {
  bucket = "wellcomecollection-platform-monitoring"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
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

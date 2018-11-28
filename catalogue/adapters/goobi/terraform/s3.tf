resource "aws_s3_bucket" "goobi_adapter" {
  bucket = "wellcomecollection-platform-adapters-goobi"

  lifecycle {
    prevent_destroy = true
  }

  # We'll probably want to remove this rule when we actually start working with
  # the Goobi adapter and this content, but while we're not using it this should
  # save a bit of money.
  lifecycle_rule {
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }
}

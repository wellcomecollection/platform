resource "aws_s3_bucket" "sierra_data" {
  bucket = "wellcomecollection-sierra-adapter-data"

  #  lifecycle_rule {
  #    id      = "records_bibs"
  #    prefix  = "records_bibs/"
  #    enabled = true
  #
  #    expiration {
  #      days = 7
  #    }
  #  }
  #
  #  lifecycle_rule {
  #    id      = "records_items"
  #    prefix  = "records_items/"
  #    enabled = true
  #
  #    expiration {
  #      days = 7
  #    }
  #  }

  lifecycle {
    prevent_destroy = true
  }
}

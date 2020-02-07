resource "aws_s3_bucket" "editorial_photography" {
  bucket = "wellcomecollection-editorial-photography"
  acl    = "private"

  lifecycle_rule {
    id      = "start_ia_move_to_glacier"
    enabled = true
    prefix  = ""

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 150
      storage_class = "GLACIER"
    }
  }

  versioning {
    enabled = true
  }

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    enabled = true

    noncurrent_version_expiration {
      days = 30
    }
  }
}

resource "aws_s3_bucket_policy" "editorial_photography" {
  bucket = aws_s3_bucket.editorial_photography.id
  policy = data.aws_iam_policy_document.editorial_photography_bucket_policy.json
}

data "aws_iam_policy_document" "editorial_photography_bucket_policy" {
  statement {
    actions = [
      "s3:ListBucket",
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]

    principals {
      identifiers = ["arn:aws:iam::299497370133:role/workflow-support"]
      type        = "AWS"
    }

    resources = [
      "${aws_s3_bucket.editorial_photography.arn}/*",
      "${aws_s3_bucket.editorial_photography.arn}",
    ]
  }

  statement {
    actions = [
      "s3:*",
    ]

    principals {
      identifiers = ["arn:aws:iam::299497370133:root"]
      type        = "AWS"
    }

    resources = [
      "${aws_s3_bucket.editorial_photography.arn}/*",
      "${aws_s3_bucket.editorial_photography.arn}",
    ]
  }

  statement {
    effect = "Deny"

    actions = [
      "s3:DeleteBucket*",
      "s3:PutBucket*",
      "s3:PutEncryptionConfiguration",
      "s3:PutInventoryConfiguration",
      "s3:PutLifecycleConfiguration",
      "s3:PutMetricsConfiguration",
      "s3:PutReplicationConfiguration",
    ]

    principals {
      identifiers = ["arn:aws:iam::299497370133:root"]
      type        = "AWS"
    }

    resources = [
      "${aws_s3_bucket.editorial_photography.arn}/*",
      "${aws_s3_bucket.editorial_photography.arn}",
    ]
  }
}

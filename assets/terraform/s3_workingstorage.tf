resource "aws_s3_bucket" "working_storage" {
  bucket = "wellcomecollection-assets-workingstorage"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  lifecycle_rule {
    id = "transition_all_to_standard_ia"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  # To protect against accidental deletions, we enable versioning on this
  # bucket and expire non-current versions after 30 days.  This gives us an
  # additional safety net against mistakes.

  versioning {
    enabled = true
  }
  lifecycle_rule {
    id = "expire_noncurrent_versions"

    noncurrent_version_expiration {
      days = 30
    }

    enabled = true
  }
}

resource "aws_s3_bucket_policy" "working_storage" {
  bucket = "${aws_s3_bucket.working_storage.id}"
  policy = "${data.aws_iam_policy_document.working_storage.json}"
}

data "aws_iam_policy_document" "working_storage" {
  statement {
    actions = [
      "s3:List*",
      "s3:Get*",
    ]

    principals {
      identifiers = ["arn:aws:iam::975596993436:root"]
      type        = "AWS"
    }

    resources = [
      "${aws_s3_bucket.working_storage.arn}",
      "${aws_s3_bucket.working_storage.arn}/*",
    ]
  }
}

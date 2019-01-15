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

  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::975596993436:root"
            },
            "Action": "s3:Get*",
            "Action": "s3:List*",
            "Resource": [
                "arn:aws:s3:::wellcomecollection-assets-workingstorage",
                "arn:aws:s3:::wellcomecollection-assets-workingstorage/*"
            ]
        }
    ]
}
POLICY
}
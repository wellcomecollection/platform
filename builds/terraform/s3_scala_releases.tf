locals {
  principals = [
    "arn:aws:iam::760097843905:root",
    "arn:aws:iam::975596993436:root",
    "arn:aws:iam::299497370133:root",
  ]
}

resource "aws_s3_bucket" "releases" {
  bucket = "releases.mvn-repo.wellcomecollection.org"
  acl    = "public-read"

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
}

resource "aws_s3_bucket_policy" "releases" {
  bucket = aws_s3_bucket.releases.id
  policy = data.aws_iam_policy_document.releases.json
}

data "aws_iam_policy_document" "releases" {
  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    principals {
      identifiers = local.principals
      type        = "AWS"
    }

    resources = [
      aws_s3_bucket.releases.arn,
      "${aws_s3_bucket.releases.arn}/*",
    ]
  }
}

resource "aws_s3_bucket_policy" "infra" {
  bucket = local.infra_bucket_id
  policy = data.aws_iam_policy_document.infra.json
}

data "aws_iam_policy_document" "infra" {
  statement {
    actions = [
      "s3:*",
    ]

    principals {
      identifiers = local.principals
      type        = "AWS"
    }

    resources = [
      "${local.infra_bucket_arn}/lambdas/*",
      "${local.infra_bucket_arn}/releases/*",
    ]
  }
}

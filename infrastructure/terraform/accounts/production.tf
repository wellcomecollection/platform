# Wellcome - Digital production team

resource "aws_s3_bucket" "client_transfer_bucket" {
  bucket = "wellcomecollection-client-transfer"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_policy" "client_transfer_read_write" {
  bucket = aws_s3_bucket.client_transfer_bucket.id
  policy = data.aws_iam_policy_document.client_transfer_read_write.json
}

data "aws_iam_policy_document" "client_transfer_read_write" {
  statement {
    resources = [
      "${aws_s3_bucket.client_transfer_bucket.arn}",
      "${aws_s3_bucket.client_transfer_bucket.arn}/*",
    ]

    actions = [
      "s3:List*",
      "s3:Get*",
      "s3:Put*",
      "s3:Delete*",
    ]

    principals {
      type = "AWS"

      identifiers = [
        "arn:aws:iam::404315009621:role/digitisation-developer",
        aws_iam_role.mediaconvert.arn,
      ]
    }
  }
}

# Mediaconvert role

resource "aws_iam_role" "mediaconvert" {
  name               = "digitisation-mediaconvert"
  assume_role_policy = data.aws_iam_policy_document.mediaconvert_assume_role_policy.json

  provider = aws.digitisation
}

resource "aws_iam_role_policy" "mediaconvert" {
  role   = aws_iam_role.mediaconvert.name
  policy = data.aws_iam_policy_document.s3.json

  provider = aws.digitisation
}

data "aws_iam_policy_document" "mediaconvert_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["mediaconvert.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "s3" {
  statement {
    resources = ["*"]
    actions   = ["s3:*"]
  }
}

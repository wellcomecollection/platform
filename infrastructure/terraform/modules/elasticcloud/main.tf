# elasticcloud snapshot reader

resource "aws_iam_role" "elasticcloud-snapshot-readonly" {
  assume_role_policy = "${data.aws_iam_policy_document.assume-elasticcloud-snapshot-readonly.json}"
}

resource "aws_iam_role_policy" "elasticcloud-snapshot-readonly" {
  policy = "${data.aws_iam_policy_document.elasticcloud-snapshot-readonly.json}"
  role = "${aws_iam_role.elasticcloud-snapshot-readonly.id}"
}

data "aws_iam_policy_document" "elasticcloud-snapshot-readonly" {
  statement {
    actions = [
      "s3:List*",
      "s3:Get*"
    ]
    resources = [
      "${data.aws_s3_bucket.elasticcloud-snapshots.arn}",
      "${data.aws_s3_bucket.elasticcloud-snapshots.arn}/*"
    ]
  }
}

data "aws_iam_policy_document" "assume-elasticcloud-snapshot-readonly" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      identifiers = ["${var.principals}"]
      type = "AWS"
    }
  }
}

# elasticcloud service

resource "aws_iam_user" "elasticcloud" {
  name = "${var.namespace}-elasticcloud"
}

resource "aws_iam_access_key" "elasticcloud" {
  user = "${aws_iam_user.elasticcloud.name}"
  pgp_key = "${var.pgp_pub_key}"
}

resource "aws_iam_user_policy" "elasticcloud" {
  user = "${aws_iam_user.elasticcloud.name}"

  policy = "${data.aws_iam_policy_document.elasticcloud.json}"
}

data "aws_s3_bucket" "elasticcloud-snapshots" {
  bucket = "${var.bucket_name}"
}

data "aws_iam_policy_document" "elasticcloud" {
  statement {
    actions = ["s3:*"]
    resources = [
      "${data.aws_s3_bucket.elasticcloud-snapshots.arn}",
      "${data.aws_s3_bucket.elasticcloud-snapshots.arn}/*"
    ]
  }
}
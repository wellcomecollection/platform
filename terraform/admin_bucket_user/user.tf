resource "aws_iam_user" "admin" {
  name = "${var.username}"
}

resource "aws_iam_user_login_profile" "admin" {
  user    = "${aws_iam_user.admin.name}"
  pgp_key = "keybase:${var.username}"
}

resource "aws_iam_access_key" "admin" {
  user    = "${aws_iam_user.admin.name}"
  pgp_key = "keybase:${var.username}"
}

resource "aws_iam_user_policy" "admin_s3_client_transfer_bucket_listbucket_r3store" {
  user   = "${aws_iam_user.admin.name}"
  policy = "${data.aws_iam_policy_document.s3_admin_transfer_bucket_listallmybuckets.json}"
}

resource "aws_iam_user_policy" "admin_s3_client_transfer_bucket_all_s3_actions_r3store" {
  user   = "${aws_iam_user.admin.name}"
  policy = "${data.aws_iam_policy_document.s3_admin_transfer_bucket_all_s3_actions.json}"
}

data "aws_iam_policy_document" "s3_admin_transfer_bucket_listallmybuckets" {
  statement {
    actions = [
      "s3:ListAllMyBuckets",
    ]

    resources = [
      "arn:aws:s3:::*",
    ]
  }
}

data "aws_iam_policy_document" "s3_admin_transfer_bucket_all_s3_actions" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.bucket_arn}",
      "${var.bucket_arn}/*",
    ]
  }
}

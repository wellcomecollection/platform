data "aws_iam_policy_document" "data_science_bucket" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.s3_bucket_arn}",
      "${var.s3_bucket_arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "data_science_readall" {
  statement {
    actions = [
      "s3:Get*",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:ListAllMyBuckets",
    ]

    resources = [
      "arn:aws:s3:::*",
    ]
  }
}

resource "aws_iam_role_policy" "p2_compute_data_science_bucket" {
  role   = "${module.p2_compute.instance_profile_role_name}"
  policy = "${data.aws_iam_policy_document.data_science_bucket.json}"
}

resource "aws_iam_role_policy" "t2_compute_data_science_bucket" {
  role   = "${module.t2_compute.instance_profile_role_name}"
  policy = "${data.aws_iam_policy_document.data_science_bucket.json}"
}

resource "aws_iam_role_policy" "p2_compute_data_science_readall" {
  role   = "${module.p2_compute.instance_profile_role_name}"
  policy = "${data.aws_iam_policy_document.data_science_readall.json}"
}

resource "aws_iam_role_policy" "t2_compute_data_science_readall" {
  role   = "${module.t2_compute.instance_profile_role_name}"
  policy = "${data.aws_iam_policy_document.data_science_readall.json}"
}

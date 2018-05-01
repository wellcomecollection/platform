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

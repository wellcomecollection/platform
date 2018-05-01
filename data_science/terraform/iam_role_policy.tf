resource "aws_iam_role_policy" "notebook_compute_data_science_bucket" {
  role   = "${module.notebook_compute.instance_profile_role_name}"
  policy = "${data.aws_iam_policy_document.data_science_bucket.json}"
}

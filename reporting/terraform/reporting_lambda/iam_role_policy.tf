resource "aws_iam_role_policy" "iam_role_policy" {
  role   = "${module.reporting_lambda.role_name}"
  policy = "${var.vhs_read_policy}"
}

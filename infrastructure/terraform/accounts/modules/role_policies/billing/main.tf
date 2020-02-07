resource "aws_iam_role_policy" "admin" {
  role   = var.role_name
  policy = data.aws_iam_policy.billing.policy
}

data "aws_iam_policy" "billing" {
  arn = "arn:aws:iam::aws:policy/job-function/Billing"
}

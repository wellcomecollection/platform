resource "aws_iam_role_policy" "admin" {
  role   = var.role_name
  policy = data.aws_iam_policy.admin.policy
}

data "aws_iam_policy" "admin" {
  arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

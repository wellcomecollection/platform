resource "aws_iam_user" "user" {
  name = "${var.prefix}-role_manager"
}

resource "aws_iam_access_key" "access_key" {
  user    = aws_iam_user.user.name
  pgp_key = var.pgp_key
}

resource "aws_iam_user_policy_attachment" "role_manager_list_roles_policy_attachement" {
  user       = aws_iam_user.user.name
  policy_arn = aws_iam_policy.list_roles.arn
}

resource "aws_iam_policy" "list_roles" {
  policy = data.aws_iam_policy_document.list_roles.json
  name   = "${var.prefix}-list_roles"
}

data "aws_iam_policy_document" "list_roles" {
  statement {
    effect    = "Allow"
    actions   = ["iam:ListRoles"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "role_assumer" {
  role   = var.role_name
  policy = data.aws_iam_policy_document.role_assumer.json
}

data "aws_iam_policy_document" "role_assumer" {
  statement {
    effect = "Allow"

    actions = [
      "sts:AssumeRole",
    ]

    resources = var.assumable_roles
  }
}

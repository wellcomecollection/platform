resource "aws_iam_user" "user" {
  name = "${var.prefix}-machine"
}

resource "aws_iam_access_key" "access_key" {
  user    = aws_iam_user.user.name
  pgp_key = var.pgp_key
}

resource "aws_iam_user_policy" "ci" {
  user   = aws_iam_user.user.name
  policy = data.aws_iam_policy_document.ci.json
}

data "aws_iam_policy_document" "ci" {
  statement {
    actions   = ["sts:AssumeRole"]
    resources = var.assumable_roles
  }
}

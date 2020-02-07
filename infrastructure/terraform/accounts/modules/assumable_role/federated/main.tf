resource "aws_iam_role" "role" {
  name               = var.name
  assume_role_policy = data.aws_iam_policy_document.assume_role.json

  max_session_duration = var.max_session_duration_in_seconds
}

data "aws_iam_policy_document" "assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Federated"
      identifiers = [var.federated_principal]
    }

    actions = ["sts:AssumeRoleWithSAML"]

    condition {
      test     = "StringEquals"
      values   = ["https://signin.aws.amazon.com/saml"]
      variable = "SAML:aud"
    }
  }

  # This statement allows you to "double hop" between assumed roles
  statement {
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = [var.aws_principal]
    }

    actions = ["sts:AssumeRole"]
  }
}

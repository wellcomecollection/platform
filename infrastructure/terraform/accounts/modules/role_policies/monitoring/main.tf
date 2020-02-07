# Role policies

resource "aws_iam_role_policy" "ecs" {
  role   = var.role_name
  policy = data.aws_iam_policy_document.ecs.json
}

resource "aws_iam_role_policy" "billing" {
  role   = var.role_name
  policy = data.aws_iam_policy_document.billing.json
}

# Policy documents

data "aws_iam_policy_document" "ecs" {
  statement {
    actions = [
      "ecs:Describe*",
      "ecs:List*",
      "ecr:Get*",
      "ecr:Describe*",
      "ecr:List*",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "billing" {
  statement {
    actions = [
      "aws-portal:ViewBilling",
      "aws-portal:ViewUsage",
    ]

    resources = [
      "*",
    ]
  }
}

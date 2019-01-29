data "aws_caller_identity" "current" {}

locals {
  ssm_arn_prefix     = "arn:aws:ssm:eu-west-1:${data.aws_caller_identity.current.account_id}:parameter/aws/reference/secretsmanager"
  secrets_arn_prefix = "arn:aws:secretsmanager:eu-west-1:${data.aws_caller_identity.current.account_id}:secret"
}

data "aws_iam_policy_document" "read_es_cluster_credentials" {
  statement {
    actions = [
      "ssm:GetParameters",
    ]

    resources = [
      "${local.ssm_arn_prefix}/catalogue/secrets/prod/es_cluster_username",
    ]
  }

  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      "${local.secrets_arn_prefix}:catalogue/secrets/prod/es_cluster_username*",
    ]
  }
}

resource "aws_iam_role_policy" "allow_es_cluster_credentials" {
  role   = "catalogue_api_gw-romulus_execution_role"
  policy = "${data.aws_iam_policy_document.read_es_cluster_credentials.json}"
}

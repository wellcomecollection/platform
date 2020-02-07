data "aws_caller_identity" "current" {}

locals {
  account_id        = data.aws_caller_identity.current.account_id
  iam_saml_provider = "${var.prefix}-saml_provider"
  principal         = "arn:aws:iam::${local.account_id}:saml-provider/${local.iam_saml_provider}"
}

# SAML

resource "aws_iam_saml_provider" "saml_provider" {
  name                   = local.iam_saml_provider
  saml_metadata_document = var.saml_xml
}

# List roles user

module "list_roles_user" {
  source = "../../users/list_roles_user"

  pgp_key = var.pgp_key
  prefix  = var.prefix
}

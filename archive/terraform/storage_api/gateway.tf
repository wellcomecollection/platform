# API

resource "aws_api_gateway_rest_api" "api" {
  name = "Storage API"

  endpoint_configuration = {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_authorizer" "cognito" {
  name          = "cognito"
  type          = "COGNITO_USER_POOLS"
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  provider_arns = ["${var.cognito_user_pool_arn}"]
}

# Stages

module "prod" {
  source      = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=v14.2.0"
  domain_name = "api.wellcomecollection.org"

  stage_name = "default"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    bags_port    = "${local.bags_listener_port}"
    ingests_port = "${local.ingests_listener_port}"
  }

  base_path = "storage"

  # All integrations
  depends_on = "${concat(module.bags.integration_uris,module.ingests.integration_uris)}"
}

# Resources

module "bags" {
  source = "resource"

  api_id    = "${aws_api_gateway_rest_api.api.id}"
  path_part = "bags"

  root_resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  connection_id    = "${aws_api_gateway_vpc_link.link.id}"

  cognito_id  = "${aws_api_gateway_authorizer.cognito.id}"
  auth_scopes = ["${var.auth_scopes}"]

  forward_port = "${local.bags_listener_port}"
}

module "ingests" {
  source = "resource"

  api_id    = "${aws_api_gateway_rest_api.api.id}"
  path_part = "ingests"

  root_resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  connection_id    = "${aws_api_gateway_vpc_link.link.id}"

  cognito_id  = "${aws_api_gateway_authorizer.cognito.id}"
  auth_scopes = ["${var.auth_scopes}"]

  forward_port = "${local.ingests_listener_port}"
}

# Link

resource "aws_api_gateway_vpc_link" "link" {
  name        = "${var.namespace}_vpc_link"
  target_arns = ["${module.nlb.arn}"]
}

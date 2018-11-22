# API

resource "aws_api_gateway_rest_api" "api" {
  name = "Catalogue API"

  endpoint_configuration = {
    types = ["REGIONAL"]
  }
}

# Stages

module "prod" {
  source      = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=v14.2.0"
  domain_name = "api.wellcomecollection.org"

  stage_name = "prod"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.prod_listener_port}"
  }

  base_path = "catalogue"

  depends_on = [
    "${module.root_resource_integration.uri}",
    "${module.simple_integration.uri}",
  ]
}

module "stage" {
  source      = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=v14.2.0"
  domain_name = "api-stage.wellcomecollection.org"

  stage_name = "stage"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.stage_listener_port}"
  }

  base_path = "catalogue"

  depends_on = [
    "${module.root_resource_integration.uri}",
    "${module.simple_integration.uri}",
  ]
}

# Resources

module "root_resource_method" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/method?ref=v14.2.0"

  api_id      = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
}

module "root_resource_integration" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/integration/proxy?ref=v14.2.0"

  api_id        = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  connection_id = "${aws_api_gateway_vpc_link.link.id}"

  hostname    = "www.example.com"
  http_method = "${module.root_resource_method.http_method}"

  forward_port = "$${stageVariables.port}"
  forward_path = "catalogue/"
}

module "simple_resource" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/resource?ref=v14.2.0"

  api_id = "${aws_api_gateway_rest_api.api.id}"

  parent_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part = "{proxy+}"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

module "simple_integration" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/integration/proxy?ref=v14.2.0"

  api_id        = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${module.simple_resource.resource_id}"
  connection_id = "${aws_api_gateway_vpc_link.link.id}"

  hostname    = "api.wellcomecollection.org"
  http_method = "${module.simple_resource.http_method}"

  forward_port = "$${stageVariables.port}"
  forward_path = "catalogue/{proxy}"

  request_parameters = {
    integration.request.path.proxy = "method.request.path.proxy"
  }
}

# Link

resource "aws_api_gateway_vpc_link" "link" {
  name        = "${var.namespace}_vpc_link"
  target_arns = ["${module.nlb.arn}"]

  lifecycle {
    create_before_destroy = true
  }
}

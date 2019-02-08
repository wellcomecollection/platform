# API

resource "aws_api_gateway_rest_api" "api" {
  name = "Catalogue API"

  endpoint_configuration = {
    types = ["REGIONAL"]
  }
}

# Stages

module "romulus" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=v17.0.0"

  stage_name = "romulus"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.romulus_listener_port}"
  }

  depends_on = [
    "${module.root_resource_integration.uri}",
    "${module.simple_integration.uri}",
  ]
}

module "remus" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=v17.0.0"

  stage_name = "remus"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.remus_listener_port}"
  }

  depends_on = [
    "${module.root_resource_integration.uri}",
    "${module.simple_integration.uri}",
  ]
}

module "v1_amber" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=v17.0.0"

  stage_name = "v1_amber"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.v1_amber_listener_port}"
  }

  depends_on = [
    "${module.root_resource_integration.uri}",
    "${module.simple_integration.uri}",
  ]
}

# Base path mappings

resource "aws_api_gateway_base_path_mapping" "catalogue_stage" {
  api_id      = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "${var.stage_api}"
  domain_name = "catalogue.api-stage.wellcomecollection.org"
  base_path   = "catalogue"
}

resource "aws_api_gateway_base_path_mapping" "catalogue_prod" {
  api_id      = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "${var.production_api}"
  domain_name = "catalogue.api.wellcomecollection.org"
  base_path   = "catalogue"
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
  source = "git::https://github.com/wellcometrust/terraform-modules.git//api_gateway/modules/resource?ref=v14.2.0"

  api_id = "${aws_api_gateway_rest_api.api.id}"

  parent_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part = "{proxy+}"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

module "simple_integration" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//api_gateway/modules/integration/proxy?ref=v14.2.0"

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

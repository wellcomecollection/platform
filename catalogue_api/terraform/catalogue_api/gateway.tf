resource "aws_api_gateway_vpc_link" "link" {
  name        = "${var.namespace}_vpc_link"
  target_arns = ["${module.nlb.arn}"]
}

resource "aws_api_gateway_rest_api" "api" {
  name = "${var.namespace}_id"

  endpoint_configuration = {
    types = ["REGIONAL"]
  }
}


module "prod" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=e74c6c83b63d68bd9afe14a0b02563477e83aa4b"

  domain_name      = "api.wellcomecollection.org"
  cert_domain_name = "api.wellcomecollection.org"

  stage_name = "prod"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.remus_listener_port}"
  }
}

module "stage" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/stage?ref=e74c6c83b63d68bd9afe14a0b02563477e83aa4b"

  domain_name      = "api-stage.wellcomecollection.org"
  cert_domain_name = "api.wellcomecollection.org"

  stage_name = "stage"
  api_id     = "${aws_api_gateway_rest_api.api.id}"

  variables = {
    port = "${local.remus_listener_port}"
  }
}

module "root_resource_method" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/method?ref=e74c6c83b63d68bd9afe14a0b02563477e83aa4b"

  api_id      = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
}

module "root_resource_integration" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/integration/proxy?ref=e74c6c83b63d68bd9afe14a0b02563477e83aa4b"

  api_id        = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  connection_id = "${aws_api_gateway_vpc_link.link.id}"

  hostname    = "www.example.com"
  http_method = "${module.root_resource_method.http_method}"

  forward_port = "$${stageVariables.port}"
  forward_path = ""
}

module "simple_resource" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/resource?ref=e74c6c83b63d68bd9afe14a0b02563477e83aa4b"

  api_id = "${aws_api_gateway_rest_api.api.id}"

  parent_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part = "{proxy+}"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

module "simple_integration" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/integration/proxy?ref=e74c6c83b63d68bd9afe14a0b02563477e83aa4b"

  api_id        = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${module.simple_resource.resource_id}"
  connection_id = "${aws_api_gateway_vpc_link.link.id}"

  hostname    = "api.wellcomecollection.org"
  http_method = "${module.simple_resource.http_method}"

  forward_port = "$${stageVariables.port}"
  forward_path = "{proxy}"

  request_parameters = {
    integration.request.path.proxy = "method.request.path.proxy"
  }
}
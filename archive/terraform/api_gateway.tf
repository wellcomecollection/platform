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
  provider_arns = ["${local.cognito_user_pool_arn}"]
}

resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "v1"
}

module "ingests" {
  source = "api_gateway_resource"

  resource_name      = "ingests"
  forward_path       = "progress"
  load_balancer_port = "${local.progress_http_lb_port}"

  vpc_link_id                    = "${aws_api_gateway_vpc_link.vpc_link.id}"
  authorizer_id                  = "${aws_api_gateway_authorizer.cognito.id}"
  storage_api_root_resource_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  storage_api_id                 = "${aws_api_gateway_rest_api.api.id}"
  cognito_storage_api_identifier = "${local.cognito_storage_api_identifier}"
}

module "bags" {
  source = "api_gateway_resource"

  resource_name      = "bags"
  forward_path       = "registrar"
  load_balancer_port = "${local.registrar_http_lb_port}"

  vpc_link_id                    = "${aws_api_gateway_vpc_link.vpc_link.id}"
  authorizer_id                  = "${aws_api_gateway_authorizer.cognito.id}"
  storage_api_root_resource_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  storage_api_id                 = "${aws_api_gateway_rest_api.api.id}"
  cognito_storage_api_identifier = "${local.cognito_storage_api_identifier}"
}

module "context" {
  source = "api_gateway_static_resource"

  resource_name      = "context.json"
  s3_key = "${aws_s3_bucket_object.context.key}"
  bucket_name = "${aws_s3_bucket.storage_static_content.id}"

  storage_api_root_resource_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  storage_api_id                 = "${aws_api_gateway_rest_api.api.id}"
  aws_region = "${var.aws_region}"
}

resource "aws_api_gateway_vpc_link" "vpc_link" {
  name        = "${local.namespace}_vpc_link"
  target_arns = ["${aws_lb.network_load_balancer.arn}"]
}

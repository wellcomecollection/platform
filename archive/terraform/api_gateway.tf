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
  resource_name = "ingests"
  authorizer_id = "${aws_api_gateway_authorizer.cognito.id}"
  load_balancer_arn = "${module.progress_http.load_balancer_arn}"
  storage_api_root_resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  forward_path = "progress"
  storage_api_id = "${aws_api_gateway_rest_api.api.id}"
  cognito_storage_api_identifier = "${local.cognito_storage_api_identifier}"
}

module "bags" {
  source = "api_gateway_resource"
  resource_name = "bags"
  authorizer_id = "${aws_api_gateway_authorizer.cognito.id}"
  load_balancer_arn = "${module.registrar_http.load_balancer_arn}"
  storage_api_root_resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  forward_path = "registrar"
  storage_api_id = "${aws_api_gateway_rest_api.api.id}"
  cognito_storage_api_identifier = "${local.cognito_storage_api_identifier}"
}
resource "aws_api_gateway_vpc_link" "progress" {
  name        = "progress_vpc_link"
  target_arns = ["${module.progress_http.load_balancer_arn}"]
}

resource "aws_api_gateway_rest_api" "api" {
  name = "Storage API V1"

  endpoint_configuration = {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_resource" "ingests" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  parent_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part   = "ingests"
}

resource "aws_api_gateway_method" "ingests" {
  rest_api_id          = "${aws_api_gateway_rest_api.api.id}"
  resource_id          = "${aws_api_gateway_resource.ingests.id}"
  http_method          = "ANY"
  authorization        = "COGNITO_USER_POOLS"
  authorizer_id        = "${aws_api_gateway_authorizer.cognito.id}"
  authorization_scopes = ["${local.cognito_storage_api_identifier}/ingests"]
}

resource "aws_api_gateway_integration" "ingests" {
  rest_api_id             = "${aws_api_gateway_rest_api.api.id}"
  resource_id             = "${aws_api_gateway_resource.ingests.id}"
  http_method             = "${aws_api_gateway_method.ingests.http_method}"
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  connection_type         = "VPC_LINK"
  connection_id           = "${aws_api_gateway_vpc_link.progress.id}"
  uri                     = "http://api.wellcomecollection.org/progress"
}

resource "aws_api_gateway_authorizer" "cognito" {
  name          = "cognito"
  type          = "COGNITO_USER_POOLS"
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  provider_arns = ["${local.cognito_user_pool_arn}"]
}

resource "aws_api_gateway_deployment" "deployment" {
  depends_on  = ["aws_api_gateway_integration.ingests"]
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "v1"
}

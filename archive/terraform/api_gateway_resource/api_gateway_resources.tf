resource "aws_api_gateway_resource" "resource" {
  rest_api_id = "${var.storage_api_id}"
  parent_id   = "${var.storage_api_root_resource_id}"
  path_part   = "${var.resource_name}"
}

resource "aws_api_gateway_method" "resource_any_method" {
  rest_api_id          = "${var.storage_api_id}"
  resource_id          = "${aws_api_gateway_resource.resource.id}"
  http_method          = "ANY"
  authorization        = "COGNITO_USER_POOLS"
  authorizer_id        = "${var.authorizer_id}"
  authorization_scopes = ["${local.authorization_scope}"]
}

resource "aws_api_gateway_integration" "resource_vpc_link_integration" {
  rest_api_id             = "${var.storage_api_id}"
  resource_id             = "${aws_api_gateway_resource.resource.id}"
  http_method             = "${aws_api_gateway_method.resource_any_method.http_method}"
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  connection_type         = "VPC_LINK"
  connection_id           = "${aws_api_gateway_vpc_link.progress.id}"
  uri                     = "${local.integration_uri}"
}

resource "aws_api_gateway_resource" "resource_subpaths" {
  rest_api_id = "${var.storage_api_id}"
  parent_id   = "${aws_api_gateway_resource.resource.id}"
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "resource_subpaths_any_method" {
  rest_api_id          = "${var.storage_api_id}"
  resource_id          = "${aws_api_gateway_resource.resource_subpaths.id}"
  http_method          = "ANY"
  authorization        = "COGNITO_USER_POOLS"
  authorizer_id        = "${var.authorizer_id}"
  authorization_scopes = ["${local.authorization_scope}"]

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

resource "aws_api_gateway_integration" "resource_subpaths_vpc_link_integration" {
  rest_api_id             = "${var.storage_api_id}"
  resource_id             = "${aws_api_gateway_resource.resource_subpaths.id}"
  http_method             = "${aws_api_gateway_method.resource_subpaths_any_method.http_method}"
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  connection_type         = "VPC_LINK"
  connection_id           = "${aws_api_gateway_vpc_link.progress.id}"
  uri                     = "${local.integration_uri}/{proxy}"
  request_parameters = {
    integration.request.path.proxy = "method.request.path.proxy"
  }
}
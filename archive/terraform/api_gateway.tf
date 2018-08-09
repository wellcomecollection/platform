resource "aws_api_gateway_rest_api" "archive_asset_lookup" {
  name        = "archive_asset_lookup"
  description = ""
}

resource "aws_api_gateway_resource" "proxy" {
  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  parent_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.root_resource_id}"
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "proxy" {
  rest_api_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  resource_id   = "${aws_api_gateway_resource.proxy.id}"
  http_method   = "ANY"
  authorization = "CUSTOM"
  authorizer_id = "NONE"
}

resource "aws_api_gateway_integration" "lambda" {
  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  resource_id = "${aws_api_gateway_resource.proxy.id}"
  http_method = "${aws_api_gateway_method.proxy.http_method}"

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${module.lambda_archive_asset_lookup.arn}"
}

resource "aws_api_gateway_method" "proxy_root" {
  rest_api_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  resource_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.root_resource_id}"
  http_method   = "ANY"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "lambda_root" {
  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  resource_id = "${aws_api_gateway_method.proxy_root.resource_id}"
  http_method = "${aws_api_gateway_method.proxy_root.http_method}"

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${module.lambda_archive_asset_lookup.invoke_arn}"
}

resource "aws_api_gateway_deployment" "test" {
  depends_on = [
    "aws_api_gateway_integration.lambda",
    "aws_api_gateway_integration.lambda_root",
  ]

  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  stage_name  = "prod"

  # keep this bit around as might require it for terraform to update the deployment
  # variables {
  #   deployed_at = "${timestamp()}"
  # }
}

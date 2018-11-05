resource "aws_api_gateway_resource" "resource" {
  rest_api_id = "${var.storage_api_id}"
  parent_id   = "${var.storage_api_root_resource_id}"
  path_part   = "${var.resource_name}"
}

resource "aws_api_gateway_method" "resource_get" {
  rest_api_id   = "${var.storage_api_id}"
  resource_id   = "${aws_api_gateway_resource.resource.id}"
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "resource_s3_integration" {
  rest_api_id             = "${var.storage_api_id}"
  resource_id             = "${aws_api_gateway_resource.resource.id}"
  http_method             = "${aws_api_gateway_method.resource_get.http_method}"
  integration_http_method = "GET"
  type                    = "AWS"
  uri                     = "arn:aws:apigateway:${var.aws_region}:s3:path//${var.bucket_name}/${var.s3_key}"
  credentials             = "${aws_iam_role.static_resource_role.arn}"
}

resource "aws_api_gateway_method_response" "200" {
  rest_api_id = "${var.storage_api_id}"
  resource_id = "${aws_api_gateway_resource.resource.id}"
  http_method = "${aws_api_gateway_method.resource_get.http_method}"
  status_code = "200"
}

resource "aws_api_gateway_integration_response" "200" {
  rest_api_id = "${var.storage_api_id}"
  resource_id = "${aws_api_gateway_resource.resource.id}"
  http_method = "${aws_api_gateway_method.resource_get.http_method}"
  status_code = "${aws_api_gateway_method_response.200.status_code}"
}

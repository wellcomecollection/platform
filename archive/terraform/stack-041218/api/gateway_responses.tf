locals {
  context_url = "https://api.wellcomecollection.org/storage/v1/context.json"
}

resource "aws_api_gateway_gateway_response" "default_4xx" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  response_type = "DEFAULT_4XX"

  response_templates = {
    "application/json" = <<EOF
{
  "@context": "${local.context_url}",
  "errorType": "http",
  "httpStatus": $context.error.responseType.statusCode,
  "label": "Client Error",
  "description": $context.error.messageString,
  "type": "Error"
}
EOF
  }
}

resource "aws_api_gateway_gateway_response" "default_5xx" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  response_type = "DEFAULT_5XX"

  response_templates = {
    "application/json" = <<EOF
{
  "@context": "${local.context_url}",
  "errorType": "http",
  "httpStatus": 500,
  "label": "Internal Server Error",
  "type": "Error"
}
EOF
  }
}

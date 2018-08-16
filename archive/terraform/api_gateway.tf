data "template_file" "archive_api_swagger" {
  template = "${file("${path.module}/archive_api_swagger.json")}"
}

resource "aws_api_gateway_rest_api" "archive_asset_lookup" {
  name = "archive_asset_lookup"
  description = "API"
  body = "${data.template_file.archive_api_swagger.rendered}"
}

resource "aws_iam_role_policy" "archive_asset_apigw" {
  role = "${module.lambda_archive_asset_lookup.role_name}"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "execute-api:Invoke",
      "Resource": [
        "arn:aws:execute-api:${var.aws_region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.archive_asset_lookup.id}/*/*/*"
      ],
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": [
            "46.102.195.182/32",
            "195.143.129.132/32"
          ]
        }
      }
    }
  ]
}
  EOF
}

//resource "aws_api_gateway_resource" "proxy" {
//  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
//  parent_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.root_resource_id}"
//  path_part   = "{proxy+}"
//}
//
//resource "aws_api_gateway_method" "proxy" {
//  rest_api_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
//  resource_id   = "${aws_api_gateway_resource.proxy.id}"
//  http_method   = "ANY"
//  authorization = "NONE"
//}
//
//resource "aws_api_gateway_integration" "lambda" {
//  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
//  resource_id = "${aws_api_gateway_resource.proxy.id}"
//  http_method = "${aws_api_gateway_method.proxy.http_method}"
//
//  integration_http_method = "POST"
//  type                    = "AWS_PROXY"
//  uri                     = "${module.lambda_archive_asset_lookup.invoke_arn}"
//}
//
//resource "aws_api_gateway_method" "proxy_root" {
//  rest_api_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
//  resource_id   = "${aws_api_gateway_rest_api.archive_asset_lookup.root_resource_id}"
//  http_method   = "ANY"
//  authorization = "NONE"
//}
//
//resource "aws_api_gateway_integration" "lambda_root" {
//  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
//  resource_id = "${aws_api_gateway_method.proxy_root.resource_id}"
//  http_method = "${aws_api_gateway_method.proxy_root.http_method}"
//
//  integration_http_method = "POST"
//  type                    = "AWS_PROXY"
//  uri                     = "${module.lambda_archive_asset_lookup.invoke_arn}"
//}
//
resource "aws_api_gateway_deployment" "test" {
  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  stage_name  = "prod"

  # keep this bit around as might require it for terraform to update the deployment
  # variables {
  #   deployed_at = "${timestamp()}"
  # }
}

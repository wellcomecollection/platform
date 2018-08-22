data "template_file" "archive_api_swagger" {
  template = "${file("${path.module}/archive_api_swagger.json")}"

  vars = {
    lookup_lambda_invoke_arn = "${module.lambda_archive_asset_lookup.invoke_arn}",
    ingest_lambda_invoke_arn = "${module.lambda_archive_ingest.invoke_arn}"
  }
}

resource "aws_api_gateway_rest_api" "archive_asset_lookup" {
  name        = "archive_asset_lookup"
  description = "API"
  body        = "${data.template_file.archive_api_swagger.rendered}"

  policy = <<POLICY
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": "*",
        "Action": "execute-api:Invoke",
        "Resource": "*",
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
  POLICY
}

resource "aws_api_gateway_deployment" "test" {
  rest_api_id = "${aws_api_gateway_rest_api.archive_asset_lookup.id}"
  stage_name  = "prod"

  # keep this bit around as might require it for terraform to update the deployment
  # variables {
  #   deployed_at = "${timestamp()}"
  # }
}

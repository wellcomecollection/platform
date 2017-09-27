resource "aws_iam_role_policy" "lambda_gatling_to_cloudwatch_put_metric" {
  role   = "${module.lambda_gatling_to_cloudwatch.role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}

resource "aws_iam_role_policy" "update_service_list_describe_services" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.terraform_remote_state.lambdas.iam_policy_document_describe_services}"
}

resource "aws_iam_role_policy" "update_service_list_push_to_s3" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_dashboard_status.json}"
}

resource "aws_iam_role_policy" "update_service_list_read_from_webplatform" {
  role = "${module.lambda_update_service_list.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "sts:AssumeRole",
      "Resource": "arn:aws:iam::130871440101:role/platform-team-assume-role"
    }
  ]
}
EOF
}

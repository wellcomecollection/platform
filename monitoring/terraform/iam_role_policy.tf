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

resource "aws_iam_role_policy" "gatling_push_to_s3" {
  role   = "${module.ecs_gatling_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_gatling_reports.json}"
}

resource "aws_iam_role_policy" "gatling_failure_alarm" {
  role   = "${module.ecs_gatling_iam.task_role_name}"
  policy = "${module.load_test_results.publish_policy}"
}

resource "aws_iam_role_policy" "gatling_results_publication" {
  role   = "${module.ecs_gatling_iam.task_role_name}"
  policy = "${module.load_test_failure_alarm.publish_policy}"
}


# grafana policies

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "ecs_grafana_task_cloudwatch_read"

  # Unfortunately grafana seems to assume the role of the ec2 instance the
  # container is running into.  This used to be a bug in grafana which was
  # fixed in version 4.3.0: https://github.com/grafana/grafana/pull/7892
  # Unfortunately we are still seeing this behaviour from the official grafana
  # docker image
  # TODO change to role = "${module.ecs_grafana_iam.task_role_name}"
  role = "${module.ecs_monitoring_iam.instance_role_name}"

  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

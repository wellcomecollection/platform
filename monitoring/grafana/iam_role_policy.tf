resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "ecs_grafana_task_cloudwatch_read"

  # Unfortunately grafana seems to assume the role of the ec2 instance the
  # container is running into.  This used to be a bug in grafana which was
  # fixed in version 4.3.0: https://github.com/grafana/grafana/pull/7892
  # Unfortunately we are still seeing this behaviour from the official grafana
  # docker image
  # TODO change to role = "${module.ecs_grafana_iam.task_role_name}"
  role = "${var.ecs_monitoring_iam_instance_role_name}"

  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

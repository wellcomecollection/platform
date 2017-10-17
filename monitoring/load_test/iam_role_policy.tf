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

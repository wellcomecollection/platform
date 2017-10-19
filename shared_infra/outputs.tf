output "lambda_error_alarm_arn" {
  value = "${module.lambda_error_alarm.arn}"
}

output "run_ecs_task_topic_arn" {
  value = "${module.run_ecs_task.arn}"
}

output "run_ecs_task_topic_publish_policy" {
  value = "${module.run_ecs_task.publish_policy}"
}

output "iam_policy_document_describe_services" {
  value = "${data.aws_iam_policy_document.describe_services.json}"
}

output "ec2_instance_terminating_for_too_long_alarm_arn" {
  value = "${module.ec2_instance_terminating_for_too_long_alarm.arn}"
}

output "dlq_alarm_arn" {
  value = "${module.dlq_alarm.arn}"
}

output "alb_server_error_alarm_arn" {
  value = "${module.alb_server_error_alarm.arn}"
}

output "alb_client_error_alarm_arn" {
  value = "${module.alb_client_error_alarm.arn}"
}

output "terminal_failure_alarm_arn" {
  value = "${module.terminal_failure_alarm.arn}"
}

output "ec2_terminating_topic_arn" {
  value = "${module.ec2_terminating_topic.arn}"
}

output "ec2_terminating_topic_publish_policy" {
  value = "${module.ec2_terminating_topic.publish_policy}"
}

output "bucket_infra_arn" {
  value = "${aws_s3_bucket.infra.arn}"
}

output "bucket_infra_id" {
  value = "${aws_s3_bucket.infra.id}"
}

output "bucket_alb_logs_id" {
  value = "${aws_s3_bucket.alb-logs.id}"
}

output "travis_ci_aws_id" {
  value = "${aws_iam_access_key.travis_ci.id}"
}

output "travis_ci_aws_secret" {
  value = "${aws_iam_access_key.travis_ci.encrypted_secret}"
}

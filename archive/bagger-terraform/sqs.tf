module "bnumbers_processing_queue" {
  source = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v11.6.0"

  queue_name  = "archive-bagger_bnumbers_processing_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.bnumbers_processing_topic.name}"]

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 300

  alarm_topic_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}

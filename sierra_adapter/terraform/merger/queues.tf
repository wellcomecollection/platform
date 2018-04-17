module "updates_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "sierra_${var.resource_type}_merger_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.updates_topic_name}"]

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 180

  # The bib merger queue has had consistent problems where the DLQ fills up,
  # and then redriving it fixes everything.  Increase the number of times a
  # message can be received before it gets marked as failed.
  max_receive_count = 8

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

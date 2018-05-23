module "demultiplexer_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "sierra_demultiplexed_items"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.demultiplexer_topic_name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"

  max_receive_count = 10
}

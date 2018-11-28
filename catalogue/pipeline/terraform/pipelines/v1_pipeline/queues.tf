module "es_ingest_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_es_ingest_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.es_ingest_topic.name}"]

  visibility_timeout_seconds = 30
  max_receive_count          = 5

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "id_minter_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_id_minter_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${var.transformed_works_topic_name}"]

  visibility_timeout_seconds = 30
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

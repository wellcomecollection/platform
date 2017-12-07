module "es_ingest_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "es_ingest_queue_${var.name}"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.es_ingest_topic.name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "id_minter_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "id_minter_queue_${var.name}"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.id_minter_topic.name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
module "miro_transformer_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "miro_transformer_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.miro_transformer_topic.name}"]

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "transformer_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v1.1.0"
  queue_name  = "transformer_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.transformer_topic.name}"]

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "es_ingest_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "es_ingest_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.es_ingest_topic.name}"]

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "id_minter_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "id_minter_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.id_minter_topic.name}"]

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

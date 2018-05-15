module "transformer_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "transformer_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.transformer_topic.name}"]

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "es_ingest_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "es_ingest_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.es_ingest_topic.name}"]

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "id_minter_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "id_minter_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.transformed_works_topic.name}"]

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "recorder_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "recorder_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.transformed_works_topic.name}"]

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

module "matcher_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "matcher_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.recorded_works_topic.name}"]

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

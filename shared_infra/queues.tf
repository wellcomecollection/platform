module "es_ingest_queue" {
  source          = "../terraform/sqs"
  queue_name      = "es_ingest_queue"
  aws_region      = "${var.aws_region}"
  account_id      = "${data.aws_caller_identity.current.account_id}"
  topic_names     = ["${module.es_ingest_topic.name}"]
  alarm_topic_arn = "${module.dlq_alarm.arn}"
}

module "id_minter_queue" {
  source          = "../terraform/sqs"
  queue_name      = "id_minter_queue"
  aws_region      = "${var.aws_region}"
  account_id      = "${data.aws_caller_identity.current.account_id}"
  topic_names     = ["${module.id_minter_topic.name}"]
  alarm_topic_arn = "${module.dlq_alarm.arn}"
}

module "miro_transformer_queue" {
  source          = "../terraform/sqs"
  queue_name      = "miro_transformer_queue"
  aws_region      = "${var.aws_region}"
  account_id      = "${data.aws_caller_identity.current.account_id}"
  topic_names     = ["${module.miro_transformer_topic.name}"]
  alarm_topic_arn = "${module.transformer_dlq_alarm.arn}"
}

module "calm_transformer_queue" {
  source          = "../terraform/sqs"
  queue_name      = "calm_transformer_queue"
  aws_region      = "${var.aws_region}"
  account_id      = "${data.aws_caller_identity.current.account_id}"
  topic_names     = ["${module.calm_transformer_topic.name}"]
  alarm_topic_arn = "${module.transformer_dlq_alarm.arn}"
}

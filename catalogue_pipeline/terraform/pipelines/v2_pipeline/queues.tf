module "es_ingest_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_es_ingest_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.es_ingest_topic.name}"]

  visibility_timeout_seconds = 30
  max_receive_count          = 6

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "id_minter_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_id_minter_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.merged_works_topic.name}"]

  visibility_timeout_seconds = 30
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "recorder_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_recorder_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = "${var.transformed_works_topic_names}"
  topic_count = "${var.transformed_works_topic_count}"

  visibility_timeout_seconds = 60
  max_receive_count          = 8

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "matcher_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_matcher_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.recorded_works_topic.name}"]

  // The records in the locktable expire after 3 minutes
  // The matcher is able to override locks that have expired
  // Wait slightly longer to make sure locks are expired
  visibility_timeout_seconds = 210

  max_receive_count = 5

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "merger_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}_merger_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.matched_works_topic.name}"]

  visibility_timeout_seconds = 30
  max_receive_count          = 4

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "ingest_queue" {
  source     = "./sqs"
  queue_name = "es_ingest_queue"
  topic_name = "${aws_sns_topic.ingest_topic.name}"
  topic_arn  = "${aws_sns_topic.ingest_topic.arn}"
  aws_region = "${var.aws_region}"
  account_id = "${data.aws_caller_identity.current.account_id}"
}

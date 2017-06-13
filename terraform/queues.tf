module "es_ingest_queue" {
  source      = "./sqs"
  queue_name  = "es_ingest_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.es_ingest_topic.name}"]
}

module "id_minter_queue" {
  source      = "./sqs"
  queue_name  = "id_minter_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.id_minter_topic.name}"]
}

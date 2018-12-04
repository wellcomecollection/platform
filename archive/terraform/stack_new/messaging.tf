# Messaging - archivist

module "ingest_requests_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_ingest_requests"
}

module "archivist_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_archivist"
}

module "archivist_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v11.6.0"
  queue_name  = "${replace(var.namespace,"-","")}_archivist"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.ingest_requests_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - registrar

module "bags_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_bags"
}

module "bags_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v11.6.0"
  queue_name  = "${replace(var.namespace,"-","")}_bags"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.bags_topic.name}"]

  visibility_timeout_seconds = 300
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - progress

module "ingests_async_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_ingests"
}

module "ingests_async_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v11.6.0"
  queue_name  = "${replace(var.namespace,"-","")}_ingests"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.ingests_async_topic.name}"]

  visibility_timeout_seconds = 180
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - notifier

module "notifier_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_notifier"
}

module "notifier_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v11.6.0"
  queue_name  = "${replace(var.namespace,"-","")}_notifier"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.notifier_topic.name}"]

  visibility_timeout_seconds = 300
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - bag creattion

module "bagger_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_bagger"
}

module "bagger_queue" {
  source = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v11.6.0"

  queue_name  = "${replace(var.namespace,"-","")}_bagger"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.bagger_topic.name}"]

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 3600

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "bagging_complete_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "${replace(var.namespace,"-","")}_bagging_complete"
}

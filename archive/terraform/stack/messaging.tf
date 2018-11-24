# Messaging - archivist

module "ingest_requests_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v16.1.2"
  name   = "${var.namespace}-ingest-requests"
}

module "archivist_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v16.1.2"
  name   = "${var.namespace}-archivist"
}

module "archivist_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}-archivist"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.ingest_requests_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - registrar

module "registrar_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v16.1.2"
  name   = "${var.namespace}-registrar"
}

module "registrar_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}-registrar"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.registrar_topic.name}"]

  visibility_timeout_seconds = 300
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - progress

module "progress_async_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v16.1.2"
  name   = "${var.namespace}-progress"
}

module "progress_async_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}-progress"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.progress_async_topic.name}"]

  visibility_timeout_seconds = 180
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - notifier

module "notifier_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v16.1.2"
  name   = "${var.namespace}_notifier_topic"
}

module "notifier_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${var.namespace}-notifier"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.notifier_topic.name}"]

  visibility_timeout_seconds = 300
  max_receive_count          = 3

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

# Messaging - bag creattion

module "bagger_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v16.1.2"
  name   = "${var.namespace}-bagger"
}

module "bagger_queue" {
  source = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v9.1.0"

  queue_name  = "${var.namespace}-bagger"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  topic_names = ["${module.bagger_topic.name}"]

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 3600

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "bagging_complete_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v16.1.2"
  name   = "${var.namespace}-bagger-complete"
}

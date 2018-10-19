# Messaging - archivist

module "archivist_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_archivist"
}

module "archivist_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_archivist_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.archivist_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - registrar

module "registrar_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_registrar"
}

module "registrar_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_registrar_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.registrar_topic.name}"]

  visibility_timeout_seconds = 300
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - progress

module "progress_async_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_progress"
}

module "progress_async_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_progress_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.progress_async_topic.name}"]

  visibility_timeout_seconds = 180
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - notifier

module "notifier_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_notifier_topic"
}

module "notifier_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_notifier_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.notifier_topic.name}"]

  visibility_timeout_seconds = 300
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - bag creattion

module "bagger_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "archive-bagger_bnumbers_processing"
}

module "bagger_queue" {
  source = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v11.6.0"

  queue_name  = "archive-bagger_bnumbers_processing_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.bagger_topic.name}"]

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 3600

  alarm_topic_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}

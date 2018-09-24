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

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - progress

module "progress_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_progress"
}

module "progress_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_progress_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.progress_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

# Messaging - caller

module "caller_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_caller"
}

# Messaging - post registration

module "registrar_completed_topic" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sns?ref=v1.0.0"
  name   = "${local.namespace}_registrar_completed"
}

module "registrar_completed_queue" {
  source      = "git::https://github.com/wellcometrust/terraform-modules.git//sqs?ref=v9.1.0"
  queue_name  = "${local.namespace}_registrar_completed_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${data.aws_caller_identity.current.account_id}"
  topic_names = ["${module.registrar_completed_topic.name}"]

  visibility_timeout_seconds = 43200
  max_receive_count          = 3

  alarm_topic_arn = "${local.dlq_alarm_arn}"
}

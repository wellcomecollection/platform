# Messaging - ingests aka progress-async

module "ingests_topic" {
  source = "../modules/topic"

  namespace = "${var.namespace}_ingests"

  role_names = [
    "${module.ingests.task_role_name}",
    "${module.bags.task_role_name}",
    "${module.archivist-nvm.task_role_name}",
    "${module.notifier.task_role_name}",
  ]
}

module "ingests_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_ingests"

  topic_names = ["${module.ingests_topic.name}"]

  aws_region = "${var.aws_region}"
  account_id = "${var.current_account_id}"

  role_names = [
    "${module.ingests.task_role_name}",
  ]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - archivist

module "ingest_requests_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_ingest_requests"
  role_names = ["${module.api.ingests_role_name}"]
}

module "archivist_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_archivist"

  topic_names = [
    "${module.ingest_requests_topic.name}",
  ]

  aws_region = "${var.aws_region}"
  account_id = "${var.current_account_id}"

  role_names = [
    "${module.archivist-nvm.task_role_name}",
  ]

  visibility_timeout_seconds = "3600"
  max_receive_count          = "1"

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - bags aka registrar-async

module "bags_topic" {
  source = "../modules/topic"

  namespace = "${var.namespace}_bags"

  role_names = [
    "${module.archivist-nvm.task_role_name}",
  ]
}

module "bags_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_bags"

  topic_names = ["${module.bags_topic.name}"]

  aws_region = "${var.aws_region}"
  account_id = "${var.current_account_id}"
  role_names = ["${module.bags.task_role_name}"]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - notifier

module "notifier_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_notifier"
  role_names = ["${module.ingests.task_role_name}"]
}

module "notifier_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_notifier"

  topic_names = ["${module.notifier_topic.name}"]

  aws_region = "${var.aws_region}"
  account_id = "${var.current_account_id}"
  role_names = ["${module.notifier.task_role_name}"]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - bagger

module "bagger_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_notifier"
  role_names = []
}

module "bagger_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_bagger"

  topic_names = ["${module.bagger_topic.name}"]

  aws_region = "${var.aws_region}"
  account_id = "${var.current_account_id}"
  role_names = ["${module.bagger-nvm.task_role_name}"]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

module "bagging_complete_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_bagging_complete"
  role_names = ["${module.bagger-nvm.task_role_name}"]
}

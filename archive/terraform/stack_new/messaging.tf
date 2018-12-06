# Messaging - api

module "ingest_requests_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_ingest_requests"
  role_names = ["${module.api.ingests_name}"]
}

# Messaging - ingests aka progress-async

module "ingests_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_ingests"
  role_names = [
    "${module.ingests.name}",
    "${module.archivist.name}",
    "${module.notifier.name}"
  ]
}

module "ingests_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_ingests"

  topic_names = ["${module.ingests_topic.name}"]

  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"

  role_names = [
    "${module.ingests.name}"
  ]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - archivist

module "archivist_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_archivist"
  role_names = ["${module.archivist.name}"]
}

module "archivist_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_archivist"

  topic_names = ["${module.archivist_topic.name}"]

  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  role_names = ["${module.archivist.name}"]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - bags aka registrar-async

module "bags_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_bags"
  role_names = ["${module.archivist.name}"]
}

module "bags_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_bags"

  topic_names = ["${module.bags_topic.name}"]

  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  role_names = ["${module.bags.name}"]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

# Messaging - notifier

module "notifier_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_notifier"
  role_names = ["${module.ingests.name}"]
}

module "notifier_queue" {
  source = "../modules/queue"

  namespace = "${replace(var.namespace,"-","")}_notifier"

  topic_names = ["${module.notifier_topic.name}"]

  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  role_names = ["${module.notifier.name}"]

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

  aws_region  = "${var.aws_region}"
  account_id  = "${var.current_account_id}"
  role_names = ["${module.bagger.name}"]

  dlq_alarm_arn = "${var.dlq_alarm_arn}"
}

module "bagging_complete_topic" {
  source = "../modules/topic"

  namespace  = "${var.namespace}_bagging_complete"
  role_names = ["${module.bagger.name}"]
}
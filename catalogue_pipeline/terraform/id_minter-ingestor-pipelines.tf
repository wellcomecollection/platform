module "ingest_pipeline_mel" {
  source = "id_minter-ingestor"

  name = "mel"

  id_minter_repository_url = "${module.ecr_repository_id_minter.repository_url}"
  ingestor_repository_url = "${module.ecr_repository_ingestor.repository_url}"
  release_ids = "${var.release_ids}"

  identifiers_rds_cluster = {
    database_name = "${module.identifiers_rds_cluster.database_name}"
    host = "${module.identifiers_rds_cluster.host}"
    port = "${module.identifiers_rds_cluster.port}"
    username = "${module.identifiers_rds_cluster.username}"
    password = "${module.identifiers_rds_cluster.password}"
  }

  cluster_name = "${aws_ecs_cluster.services.name}"
  vpc_id = "${module.vpc_services.vpc_id}"

  services_alb = {
    cloudwatch_id = "${module.services_alb.cloudwatch_id}"
    listener_https_arn = "${module.services_alb.listener_https_arn}"
    listener_http_arn = "${module.services_alb.listener_http_arn}"
  }

  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_server_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  account_id = "${data.aws_caller_identity.current.account_id}"
  aws_region = "${var.aws_region}"
  dlq_alarm_arn = "${local.dlq_alarm_arn}"

  es_config_ingestor = {
    name = "${var.es_config_ingestor_mel["name"]}"
    region = "${var.es_config_ingestor_mel["region"]}"
    port = "${var.es_config_ingestor_mel["port"]}"
    name = "${var.es_config_ingestor_mel["name"]}"
    index = "${var.es_config_ingestor_mel["index"]}"
    doc_type = "${var.es_config_ingestor_mel["doc_type"]}"
    username = "${var.es_config_ingestor_mel["username"]}"
    password = "${var.es_config_ingestor_mel["password"]}"
    protocol = "${var.es_config_ingestor_mel["protocol"]}"
  }

  cloudwatch_push_metrics_policy_document = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
  infra_bucket = "${var.infra_bucket}"
}

module "ingest_pipeline_sue" {
  source = "id_minter-ingestor"

  name = "sue"

  id_minter_repository_url = "${module.ecr_repository_id_minter.repository_url}"
  ingestor_repository_url = "${module.ecr_repository_ingestor.repository_url}"
  release_ids = "${var.release_ids}"

  identifiers_rds_cluster = {
    database_name = "${module.identifiers_rds_cluster.database_name}"
    host = "${module.identifiers_rds_cluster.host}"
    port = "${module.identifiers_rds_cluster.port}"
    username = "${module.identifiers_rds_cluster.username}"
    password = "${module.identifiers_rds_cluster.password}"
  }

  cluster_name = "${aws_ecs_cluster.services.name}"
  vpc_id = "${module.vpc_services.vpc_id}"

  services_alb = {
    cloudwatch_id = "${module.services_alb.cloudwatch_id}"
    listener_https_arn = "${module.services_alb.listener_https_arn}"
    listener_http_arn = "${module.services_alb.listener_http_arn}"
  }

  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_server_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  account_id = "${data.aws_caller_identity.current.account_id}"
  aws_region = "${var.aws_region}"
  dlq_alarm_arn = "${local.dlq_alarm_arn}"

  es_config_ingestor = {
    name = "${var.es_config_ingestor_sue["name"]}"
    region = "${var.es_config_ingestor_sue["region"]}"
    port = "${var.es_config_ingestor_sue["port"]}"
    name = "${var.es_config_ingestor_sue["name"]}"
    index = "${var.es_config_ingestor_sue["index"]}"
    doc_type = "${var.es_config_ingestor_sue["doc_type"]}"
    username = "${var.es_config_ingestor_sue["username"]}"
    password = "${var.es_config_ingestor_sue["password"]}"
    protocol = "${var.es_config_ingestor_sue["protocol"]}"
  }

  cloudwatch_push_metrics_policy_document = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
  infra_bucket = "${var.infra_bucket}"
}
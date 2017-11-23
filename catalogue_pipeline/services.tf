module "miro_reindexer" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "miro_reindexer"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_miro_reindexer_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_reindexer.repository_url}:${var.release_ids["reindexer"]}"
  nginx_uri          = "${module.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/miro_reindexer/*"
  alb_priority       = "104"
  healthcheck_path   = "/miro_reindexer/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/miro_reindexer.ini"

  cpu    = 512
  memory = 1024

  desired_count = "0"

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    miro_table_name    = "${aws_dynamodb_table.miro_table.name}"
    reindex_table_name = "${aws_dynamodb_table.reindex_tracker.name}"
    metrics_namespace  = "miro-reindexer"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}

data "template_file" "es_cluster_host_ingestor" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_ingestor["name"]}"
    region = "${var.es_config_ingestor["region"]}"
  }
}

module "ingestor" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "ingestor"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_ingestor_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_ingestor.repository_url}:${var.release_ids["ingestor"]}"
  nginx_uri          = "${module.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/ingestor/*"
  alb_priority       = "102"
  healthcheck_path   = "/ingestor/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/ingestor.ini"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    es_host           = "${data.template_file.es_cluster_host_ingestor.rendered}"
    es_port           = "${var.es_config_ingestor["port"]}"
    es_name           = "${var.es_config_ingestor["name"]}"
    es_index          = "${var.es_config_ingestor["index"]}"
    es_doc_type       = "${var.es_config_ingestor["doc_type"]}"
    es_username       = "${var.es_config_ingestor["username"]}"
    es_password       = "${var.es_config_ingestor["password"]}"
    es_protocol       = "${var.es_config_ingestor["protocol"]}"
    ingest_queue_id   = "${module.es_ingest_queue.id}"
    metrics_namespace = "ingestor"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}

module "transformer" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "transformer"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_transformer_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_transformer.repository_url}:${var.release_ids["transformer"]}"
  nginx_uri          = "${module.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/transformer/*"
  alb_priority       = "100"
  healthcheck_path   = "/transformer/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/transformer.ini"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    sns_arn              = "${module.id_minter_topic.arn}"
    transformer_queue_id = "${module.miro_transformer_queue.id}"
    source_table_name    = "${aws_dynamodb_table.miro_table.name}"
    metrics_namespace    = "miro-transformer"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}


module "id_minter_sqs_appautoscaling" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_sqs_appautoscaling?ref=ecs-sqs-autoscaling-policy"
  name    = "id_minter"

  queue_name   = "${module.id_minter_queue.id}"
  cluster_name = "${aws_ecs_cluster.services.name}"
  service_name = "${module.id_minter.service_name}"

  min_capacity = 0
  max_capacity = 1
}

module "id_minter" {
  source             = "git::https://github.com/wellcometrust/terraform.git//services?ref=v1.0.0"
  name               = "id_minter"
  cluster_id         = "${aws_ecs_cluster.services.id}"
  task_role_arn      = "${module.ecs_id_minter_iam.task_role_arn}"
  vpc_id             = "${module.vpc_services.vpc_id}"
  app_uri            = "${module.ecr_repository_id_minter.repository_url}:${var.release_ids["id_minter"]}"
  nginx_uri          = "${module.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
  listener_https_arn = "${module.services_alb.listener_https_arn}"
  listener_http_arn  = "${module.services_alb.listener_http_arn}"
  path_pattern       = "/id_minter/*"
  alb_priority       = "103"
  healthcheck_path   = "/id_minter/management/healthcheck"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/id_minter.ini"

  cpu    = 256
  memory = 1024

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  config_vars = {
    rds_database_name   = "${module.identifiers_rds_cluster.database_name}"
    rds_host            = "${module.identifiers_rds_cluster.host}"
    rds_port            = "${module.identifiers_rds_cluster.port}"
    rds_username        = "${module.identifiers_rds_cluster.username}"
    rds_password        = "${module.identifiers_rds_cluster.password}"
    id_minter_queue_id  = "${module.id_minter_queue.id}"
    es_ingest_topic_arn = "${module.es_ingest_topic.arn}"
    metrics_namespace   = "id-minter"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${local.alb_server_error_alarm_arn}"
  client_error_alarm_topic_arn = "${local.alb_client_error_alarm_arn}"
}

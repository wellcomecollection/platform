module "miro_reindexer" {
  source             = "../terraform/services"
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
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

data "template_file" "es_cluster_host_ingestor" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_ingestor["name"]}"
    region = "${var.es_config_ingestor["region"]}"
  }
}

module "ingestor" {
  source             = "../terraform/services"
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
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "transformer" {
  source             = "../terraform/services"
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
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "id_minter" {
  source             = "../terraform/services"
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
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

data "template_file" "es_cluster_host_romulus" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_romulus["name"]}"
    region = "${var.es_config_romulus["region"]}"
  }
}

module "api_romulus_v1" {
  source             = "../terraform/services"
  name               = "api_romulus_v1"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  task_role_arn      = "${module.ecs_api_iam.task_role_arn}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_api.repository_url}:${var.pinned_romulus_api != "" ? var.pinned_romulus_api : var.release_ids["api"]}"
  nginx_uri          = "${module.ecr_repository_nginx_api.repository_url}:${var.pinned_romulus_api_nginx != "" ? var.pinned_romulus_api_nginx : var.release_ids["nginx_api"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/api_romulus_v1.ini"
  path_pattern       = "/catalogue/v1/*"
  alb_priority       = "114"
  host_name          = "${var.production_api == "romulus" ? var.api_host : var.api_host_stage}"

  enable_alb_alarm = "${var.production_api == "romulus" ? 1 : 0}"

  cpu    = 1792
  memory = 2048

  desired_count = "${var.production_api == "romulus" ? var.api_task_count : var.api_task_count_stage}"

  deployment_minimum_healthy_percent = "${var.production_api == "romulus" ? "50" : "0"}"
  deployment_maximum_percent         = "200"

  config_vars = {
    api_host    = "${var.api_host}"
    es_host     = "${data.template_file.es_cluster_host_romulus.rendered}"
    es_port     = "${var.es_config_romulus["port"]}"
    es_name     = "${var.es_config_romulus["name"]}"
    es_index    = "${var.es_config_romulus["index"]}"
    es_doc_type = "${var.es_config_romulus["doc_type"]}"
    es_username = "${var.es_config_romulus["username"]}"
    es_password = "${var.es_config_romulus["password"]}"
    es_protocol = "${var.es_config_romulus["protocol"]}"
  }

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

data "template_file" "es_cluster_host_remus" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config_remus["name"]}"
    region = "${var.es_config_remus["region"]}"
  }
}

module "api_remus_v1" {
  source             = "../terraform/services"
  name               = "api_remus_v1"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  task_role_arn      = "${module.ecs_api_iam.task_role_arn}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_api.repository_url}:${var.pinned_remus_api != "" ? var.pinned_remus_api : var.release_ids["api"]}"
  nginx_uri          = "${module.ecr_repository_nginx_api.repository_url}:${var.pinned_remus_api_nginx != "" ? var.pinned_remus_api_nginx : var.release_ids["nginx_api"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/api_remus_v1.ini"
  path_pattern       = "/catalogue/v1/*"
  alb_priority       = "113"
  host_name          = "${var.production_api == "remus" ? var.api_host : var.api_host_stage}"

  enable_alb_alarm = "${var.production_api == "remus" ? 1 : 0}"

  cpu    = 1792
  memory = 2048

  desired_count = "${var.production_api == "remus" ? var.api_task_count : var.api_task_count_stage}"

  deployment_minimum_healthy_percent = "${var.production_api == "remus" ? "50" : "0"}"
  deployment_maximum_percent         = "200"

  config_vars = {
    api_host    = "${var.api_host}"
    es_host     = "${data.template_file.es_cluster_host_remus.rendered}"
    es_port     = "${var.es_config_remus["port"]}"
    es_name     = "${var.es_config_remus["name"]}"
    es_index    = "${var.es_config_remus["index"]}"
    es_doc_type = "${var.es_config_remus["doc_type"]}"
    es_username = "${var.es_config_remus["username"]}"
    es_password = "${var.es_config_remus["password"]}"
    es_protocol = "${var.es_config_remus["protocol"]}"
  }

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "grafana" {
  source             = "../terraform/services"
  name               = "grafana"
  cluster_id         = "${aws_ecs_cluster.monitoring.id}"
  task_role_arn      = "${module.ecs_grafana_iam.task_role_arn}"
  vpc_id             = "${module.vpc_monitoring.vpc_id}"
  listener_https_arn = "${module.monitoring_alb.listener_https_arn}"
  listener_http_arn  = "${module.monitoring_alb.listener_http_arn}"

  cpu    = 256
  memory = 256

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  nginx_uri                = "${module.ecr_repository_nginx_grafana.repository_url}:${var.release_ids["nginx_grafana"]}"
  healthcheck_path         = "/api/health"
  secondary_container_port = "3000"
  app_uri                  = "grafana/grafana:4.4.3"

  volume_name      = "grafana"
  volume_host_path = "${module.monitoring_userdata.efs_mount_directory}/grafana"
  container_path   = "/var/lib/grafana"

  extra_vars = [
    "{\"name\" : \"GF_AUTH_ANONYMOUS_ENABLED\", \"value\" : \"${var.grafana_anonymous_enabled}\"}",
    "{\"name\" : \"GF_AUTH_ANONYMOUS_ORG_ROLE\", \"value\" : \"${var.grafana_anonymous_role}\"}",
    "{\"name\" : \"GF_SECURITY_ADMIN_USER\", \"value\" : \"${var.grafana_admin_user}\"}",
    "{\"name\" : \"GF_SECURITY_ADMIN_PASSWORD\", \"value\" : \"${var.grafana_admin_password}\"}",
  ]

  config_key        = ""
  infra_bucket      = ""
  is_config_managed = false

  loadbalancer_cloudwatch_id   = "${module.monitoring_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

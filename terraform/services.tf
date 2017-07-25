data "template_file" "es_cluster_host" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config["name"]}"
    region = "${var.es_config["region"]}"
  }
}

module "miro_reindexer" {
  source             = "./services"
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

  config_vars = {
    miro_table_name    = "${aws_dynamodb_table.miro_table.name}"
    reindex_table_name = "${aws_dynamodb_table.reindex_tracker.name}"
    metrics_namespace  = "miro-reindexer"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "ingestor" {
  source             = "./services"
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

  config_vars = {
    es_host           = "${data.template_file.es_cluster_host.rendered}"
    es_port           = "${var.es_config["port"]}"
    es_name           = "${var.es_config["name"]}"
    es_index          = "${var.es_config["index"]}"
    es_doc_type       = "${var.es_config["doc_type"]}"
    es_username       = "${var.es_config["username"]}"
    es_password       = "${var.es_config["password"]}"
    es_protocol       = "${var.es_config["protocol"]}"
    ingest_queue_id   = "${module.es_ingest_queue.id}"
    metrics_namespace = "ingestor"
  }

  loadbalancer_cloudwatch_id   = "${module.services_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "transformer" {
  source             = "./services"
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
  source             = "./services"
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

module "api" {
  source             = "./services"
  name               = "api"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  task_role_arn      = "${module.ecs_api_iam.task_role_arn}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_api.repository_url}:${var.release_ids["api"]}"
  nginx_uri          = "${module.ecr_repository_nginx_api.repository_url}:${var.release_ids["nginx_api"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/api.ini"
  alb_priority       = "110"
  host_name          = "api.wellcomecollection.org"

  config_vars = {
    api_host    = "${var.api_host}"
    es_host     = "${data.template_file.es_cluster_host.rendered}"
    es_port     = "${var.es_config["port"]}"
    es_name     = "${var.es_config["name"]}"
    es_index    = "${var.es_config["index"]}"
    es_doc_type = "${var.es_config["doc_type"]}"
    es_username = "${var.es_config["username"]}"
    es_password = "${var.es_config["password"]}"
    es_protocol = "${var.es_config["protocol"]}"
  }

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "loris" {
  source             = "./services"
  name               = "loris"
  cluster_id         = "${aws_ecs_cluster.api.id}"
  task_role_arn      = "${module.ecs_loris_iam.task_role_arn}"
  vpc_id             = "${module.vpc_api.vpc_id}"
  app_uri            = "${module.ecr_repository_loris.repository_url}:latest"
  nginx_uri          = "${module.ecr_repository_nginx_loris.repository_url}:${var.release_ids["nginx_loris"]}"
  listener_https_arn = "${module.api_alb.listener_https_arn}"
  listener_http_arn  = "${module.api_alb.listener_http_arn}"
  infra_bucket       = "${var.infra_bucket}"
  config_key         = "config/${var.build_env}/loris.ini"
  path_pattern       = "/image*"
  healthcheck_path   = "/image/"
  alb_priority       = "109"
  host_name          = "iiif-origin.wellcomecollection.org"

  cpu = 1280

  desired_count = "2"

  deployment_minimum_healthy_percent = "50"
  deployment_maximum_percent         = "200"

  volume_name      = "loris"
  volume_host_path = "${module.api_userdata.efs_mount_directory}/loris"
  container_path   = "/usr/local/share/images/loris"

  loadbalancer_cloudwatch_id   = "${module.api_alb.cloudwatch_id}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
}

module "grafana" {
  source             = "./services"
  name               = "grafana"
  cluster_id         = "${aws_ecs_cluster.monitoring.id}"
  task_role_arn      = "${module.ecs_grafana_iam.task_role_arn}"
  vpc_id             = "${module.vpc_monitoring.vpc_id}"
  listener_https_arn = "${module.monitoring_alb.listener_https_arn}"
  listener_http_arn  = "${module.monitoring_alb.listener_http_arn}"

  cpu    = 256
  memory = 1024

  nginx_uri                = "${module.ecr_repository_nginx_grafana.repository_url}:${var.release_ids["nginx_grafana"]}"
  healthcheck_path         = "/api/health"
  secondary_container_port = "3000"
  app_uri                  = "grafana/grafana"

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

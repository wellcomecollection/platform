data "template_file" "es_cluster_host" {
  template = "$${name}.$${region}.aws.found.io"

  vars {
    name   = "${var.es_config["name"]}"
    region = "${var.es_config["region"]}"
  }
}

module "calm_adapter" {
  source           = "./services"
  name             = "calm_adapter"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_calm_adapter_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.calm_adapter.repository_url}:${var.release_ids["calm_adapter"]}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/calm_adapter/*"
  alb_priority     = "101"
  healthcheck_path = "/calm_adapter/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  # The Calm adapter is disabled when not running.  It only runs once a day
  # because the last_changed date on Calm records has per-day granularity.
  #
  # A scheduled Lambda sets the desired count to 1 on weekdays, and the
  # adapter resets it to zero when it finished running.
  desired_count = "0"

  config_vars = {
    table_name = "${aws_dynamodb_table.calm_table.name}"
    sns_arn    = "${module.service_scheduler_topic.arn}"
  }
}

module "miro_reindexer" {
  source           = "./services"
  name             = "miro_reindexer"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_miro_reindexer_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.calm_adapter.repository_url}:${var.release_ids["reindexer"]}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/miro_reindexer/*"
  alb_priority     = "104"
  healthcheck_path = "/miro_reindexer/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  desired_count = "0"

  config_vars = {
    miro_table_name    = "${aws_dynamodb_table.miro_table.name}"
    reindex_table_name = "${aws_dynamodb_table.reindex_tracker.name}"
  }
}

module "ingestor" {
  source           = "./services"
  name             = "ingestor"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_ingestor_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.ingestor.repository_url}:${var.release_ids["ingestor"]}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/ingestor/*"
  alb_priority     = "102"
  healthcheck_path = "/ingestor/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    es_host         = "${data.template_file.es_cluster_host.rendered}"
    es_port         = "${var.es_config["port"]}"
    es_name         = "${var.es_config["name"]}"
    es_index        = "${var.es_config["index"]}"
    es_doc_type     = "${var.es_config["doc_type"]}"
    es_xpack_user   = "${var.es_config["xpack_user"]}"
    ingest_queue_id = "${module.es_ingest_queue.id}"
  }
}

module "transformer" {
  source           = "./services"
  name             = "transformer"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_transformer_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.transformer.repository_url}:${var.release_ids["transformer"]}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/transformer/*"
  alb_priority     = "100"
  healthcheck_path = "/transformer/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    stream_arn        = "${aws_dynamodb_table.miro_table.stream_arn}"
    sns_arn           = "${module.id_minter_topic.arn}"
    metrics_namespace = "miro-transformer"
  }
}

module "id_minter" {
  source           = "./services"
  name             = "id_minter"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_id_minter_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.id_minter.repository_url}:${var.release_ids["id_minter"]}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/id_minter/*"
  alb_priority     = "103"
  healthcheck_path = "/id_minter/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    id_minter_queue_id  = "${module.id_minter_queue.id}"
    es_ingest_topic_arn = "${module.es_ingest_topic.arn}"
    table_name          = "${aws_dynamodb_table.identifiers.name}"
  }
}

module "api" {
  source        = "./services"
  name          = "api"
  cluster_id    = "${aws_ecs_cluster.api.id}"
  task_role_arn = "${module.ecs_api_iam.task_role_arn}"
  vpc_id        = "${module.vpc_api.vpc_id}"
  app_uri       = "${aws_ecr_repository.api.repository_url}:${var.release_ids["api"]}"
  nginx_uri     = "${aws_ecr_repository.nginx.repository_url}:api"
  listener_arn  = "${module.api_alb.listener_arn}"
  infra_bucket  = "${var.infra_bucket}"

  config_vars = {
    api_host      = "${var.api_host}"
    es_host       = "${data.template_file.es_cluster_host.rendered}"
    es_port       = "${var.es_config["port"]}"
    es_name       = "${var.es_config["name"]}"
    es_xpack_user = "${var.es_config["xpack_user"]}"
    es_index      = "${var.es_config["index"]}"
    es_doc_type   = "${var.es_config["doc_type"]}"
  }
}

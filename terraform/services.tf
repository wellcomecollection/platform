module "calm_adapter" {
  source           = "./services"
  name             = "calm_adapter"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_calm_adapter_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.calm_adapter.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/calm_adapter/*"
  alb_priority     = "101"
  desired_count    = "0"
  healthcheck_path = "/calm_adapter/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    table_name = "${aws_dynamodb_table.calm_table.name}"
    sns_arn    = "${aws_sns_topic.service_scheduler_topic.arn}"
  }
}

module "ingestor" {
  source           = "./services"
  name             = "ingestor"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_ingestor_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.ingestor.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/ingestor/*"
  alb_priority     = "102"
  healthcheck_path = "/ingestor/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    es_host         = "${var.es_config["host"]}"
    es_port         = "${var.es_config["port"]}"
    es_name         = "${var.es_config["name"]}"
    es_index        = "${var.es_config["index"]}"
    es_doc_type     = "${var.es_config["doc_type"]}"
    es_xpack_user   = "${var.es_config["xpack_user"]}"
    ingest_queue_id = "${module.ingest_queue.id}"
  }
}

module "transformer" {
  source           = "./services"
  name             = "transformer"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_transformer_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.transformer.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/transformer/*"
  alb_priority     = "100"
  healthcheck_path = "/transformer/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    stream_arn = "${aws_dynamodb_table.miro_table.stream_arn}"
    sns_arn    = "${aws_sns_topic.id_minter_topic.arn}"
  }
}

module "id_minter" {
  source           = "./services"
  name             = "id_minter"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_role_arn    = "${module.ecs_id_minter_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.id_minter.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/id_minter/*"
  alb_priority     = "103"
  healthcheck_path = "/id_minter/management/healthcheck"
  infra_bucket     = "${var.infra_bucket}"

  config_vars = {
    id_minter_queue_id  = "${module.id_minter_queue.id}"
    es_ingest_topic_arn = "${aws_sns_topic.ingest_topic.arn}"
    table_name          = "${aws_dynamodb_table.identifiers.name}"
  }
}

module "api" {
  source        = "./services"
  name          = "api"
  cluster_id    = "${aws_ecs_cluster.api.id}"
  task_role_arn = "${module.ecs_api_iam.task_role_arn}"
  vpc_id        = "${module.vpc_api.vpc_id}"
  app_uri       = "${aws_ecr_repository.api.repository_url}:${var.release_id}"
  nginx_uri     = "${aws_ecr_repository.nginx.repository_url}:api"
  listener_arn  = "${module.api_alb.listener_arn}"
  infra_bucket  = "${var.infra_bucket}"

  config_vars = {
    es_host       = "${var.es_config["host"]}"
    es_port       = "${var.es_config["port"]}"
    es_name       = "${var.es_config["name"]}"
    es_xpack_user = "${var.es_config["xpack_user"]}"
  }
}

module "jenkins" {
  source           = "./services"
  name             = "jenkins"
  cluster_id       = "${aws_ecs_cluster.tools.id}"
  task_role_arn    = "${module.ecs_tools_iam.task_role_arn}"
  container_name   = "jenkins"
  container_port   = "8080"
  vpc_id           = "${module.vpc_tools.vpc_id}"
  volume_name      = "jenkins-home"
  volume_host_path = "/mnt/efs"
  template_name    = "jenkins"
  listener_arn     = "${module.tools_alb.listener_arn}"
  healthcheck_path = "/login"
  infra_bucket     = "${var.infra_bucket}"
}

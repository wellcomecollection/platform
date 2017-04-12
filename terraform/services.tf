module "calm_adapter" {
  source           = "./services"
  service_name     = "calm-adapter"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_name        = "calm_adapter"
  task_role_arn    = "${module.ecs_calm_adapter_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.calm_adapter.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/calm_adapter/*"
  alb_priority     = "101"
  desired_count    = "0"
  healthcheck_path = "/calm_adapter/management/healthcheck"
}

module "ingestor" {
  source           = "./services"
  service_name     = "ingestor"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_name        = "ingestor"
  task_role_arn    = "${module.ecs_ingestor_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.ingestor.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/ingestor/*"
  alb_priority     = "102"
  healthcheck_path = "/ingestor/management/healthcheck"
}

module "transformer" {
  source           = "./services"
  service_name     = "transformer"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_name        = "transformer"
  task_role_arn    = "${module.ecs_transformer_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.transformer.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/transformer/*"
  alb_priority     = "100"
  healthcheck_path = "/transformer/management/healthcheck"
}

module "id_minter" {
  source           = "./services"
  service_name     = "id-minter"
  cluster_id       = "${aws_ecs_cluster.services.id}"
  task_name        = "id_minter"
  task_role_arn    = "${module.ecs_transformer_iam.task_role_arn}"
  vpc_id           = "${module.vpc_services.vpc_id}"
  app_uri          = "${aws_ecr_repository.id_minter.repository_url}:${var.release_id}"
  nginx_uri        = "${aws_ecr_repository.nginx.repository_url}:services"
  listener_arn     = "${module.services_alb.listener_arn}"
  path_pattern     = "/id_minter/*"
  alb_priority     = "103"
  healthcheck_path = "/id_minter/management/healthcheck"
}

module "api" {
  source        = "./services"
  service_name  = "api"
  cluster_id    = "${aws_ecs_cluster.api.id}"
  task_name     = "api"
  task_role_arn = "${module.ecs_api_iam.task_role_arn}"
  vpc_id        = "${module.vpc_api.vpc_id}"
  app_uri       = "${aws_ecr_repository.api.repository_url}:${var.release_id}"
  nginx_uri     = "${aws_ecr_repository.nginx.repository_url}:api"
  listener_arn  = "${module.api_alb.listener_arn}"
}

module "jenkins" {
  source           = "./services"
  service_name     = "jenkins"
  cluster_id       = "${aws_ecs_cluster.tools.id}"
  task_name        = "jenkins"
  task_role_arn    = "${module.ecs_tools_iam.task_role_arn}"
  container_name   = "jenkins"
  container_port   = "8080"
  vpc_id           = "${module.vpc_tools.vpc_id}"
  volume_name      = "jenkins-home"
  volume_host_path = "/mnt/efs"
  template_name    = "jenkins"
  listener_arn     = "${module.tools_alb.listener_arn}"
  healthcheck_path = "/login"
}

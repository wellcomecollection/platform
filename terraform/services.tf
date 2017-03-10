module "api" {
  source         = "./services"
  service_name   = "api"
  cluster_id     = "${aws_ecs_cluster.main.id}"
  task_name      = "api"
  task_role_arn  = "${module.ecs_main_iam.task_role_arn}"
  cert_arn       = "${data.aws_acm_certificate.api.arn}"
  container_name = "api"
  container_port = "8888"
  vpc_id         = "${module.vpc_main.vpc_id}"
  alb_id         = "${aws_alb.main.id}"
  image_uri      = "${aws_ecr_repository.api.repository_url}:${var.release_id}"
}

module "jenkins" {
  source           = "./services"
  service_name     = "jenkins"
  cluster_id       = "${aws_ecs_cluster.tools.id}"
  task_name        = "jenkins"
  task_role_arn    = "${module.ecs_tools_iam.task_role_arn}"
  cert_arn         = "${data.aws_acm_certificate.tools.arn}"
  container_name   = "jenkins"
  container_port   = "8080"
  vpc_id           = "${module.vpc_tools.vpc_id}"
  alb_id           = "${aws_alb.tools.id}"
  volume_name      = "jenkins-home"
  volume_host_path = "/mnt/efs"
  template_name    = "jenkins"
}

module "api" {
  source              = "./ecs_service"
  service_name        = "api"
  cluster_id          = "${aws_ecs_cluster.main.id}"
  task_definition_arn = "${aws_ecs_task_definition.platform_api.arn}"
  alb_id              = "${aws_alb.main.id}"
  vpc_id              = "${module.vpc_main.vpc_id}"
  acm_cert_arn        = "${data.aws_acm_certificate.api.arn}"
  container_name      = "${data.template_file.api_task_definition.vars.container_name}"
  container_port      = "8888"
}

module "jenkins" {
  source              = "./ecs_service"
  service_name        = "jenkins"
  cluster_id          = "${aws_ecs_cluster.tools.id}"
  task_definition_arn = "${aws_ecs_task_definition.jenkins.arn}"
  alb_id              = "${aws_alb.tools.id}"
  vpc_id              = "${module.vpc_tools.vpc_id}"
  acm_cert_arn        = "${data.aws_acm_certificate.tools.arn}"
  container_name      = "${data.template_file.jenkins_task_definition.vars.container_name}"
  container_port      = "8080"
}

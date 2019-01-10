# EC2 ECS Host

module "ec2_efs_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/ec2/prebuilt/efs?ref=8f733b7"

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  asg_name = "${var.namespace}"

  ssh_ingress_security_groups = []

  custom_security_groups = [
    "${var.efs_security_group_id}",
  ]

  subnets  = "${var.private_subnets}"
  key_name = "${var.key_name}"

  efs_fs_id = "${var.efs_id}"
  region    = "${var.aws_region}"
}

# Service

module "task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/efs?ref=v11.0.0"

  aws_region = "${var.aws_region}"
  task_name  = "${var.namespace}"

  container_image = "grafana/grafana:${var.grafana_version}"
  container_port  = "3000"

  efs_host_path      = "${module.ec2_efs_host.efs_host_path}/grafana"
  efs_container_path = "/var/lib/grafana"

  cpu    = 256
  memory = 512

  env_vars = {
    GF_AUTH_ANONYMOUS_ENABLED  = "${var.grafana_anonymous_enabled}"
    GF_AUTH_ANONYMOUS_ORG_ROLE = "${var.grafana_anonymous_role}"
    GF_SECURITY_ADMIN_USER     = "${var.grafana_admin_user}"
    GF_SECURITY_ADMIN_PASSWORD = "${var.grafana_admin_password}"
  }
}

module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v11.0.0"

  service_name       = "${var.namespace}"
  task_desired_count = "1"

  security_group_ids = [
    "${aws_security_group.service_lb_security_group.id}",
    "${aws_security_group.service_egress_security_group.id}",
  ]

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  ecs_cluster_id = "${var.cluster_id}"

  vpc_id = "${var.vpc_id}"

  subnets = [
    "${var.private_subnets}",
  ]

  namespace_id = "${var.namespace_id}"

  container_port = "${module.task.container_port}"
  container_name = "${module.task.container_name}"

  task_definition_arn = "${module.task.task_definition_arn}"

  healthcheck_path = "/api/health"

  launch_type = "EC2"
}

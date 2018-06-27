module "grafana_task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/efs?ref=v11.0.0"

  aws_region = "${var.aws_region}"
  task_name  = "${local.namespace}_ec2_private_efs"

  container_image = "grafana/grafana:5.2.0"
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

module "grafana_service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=v11.0.0"

  service_name       = "grafana_v2"
  task_desired_count = "1"

  security_group_ids = [
    "${aws_security_group.service_lb_security_group.id}",
    "${aws_security_group.service_egress_security_group.id}",
  ]

  deployment_minimum_healthy_percent = "0"
  deployment_maximum_percent         = "200"

  ecs_cluster_id = "${aws_ecs_cluster.monitoring.id}"

  vpc_id = "${module.network.vpc_id}"

  subnets = [
    "${module.network.private_subnets}",
  ]

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  container_port = "${module.grafana_task.container_port}"
  container_name = "${module.grafana_task.container_name}"

  task_definition_arn = "${module.grafana_task.task_definition_arn}"

  healthcheck_path = "/api/health"

  launch_type = "EC2"
}

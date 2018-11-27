# Service

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

  vpc_id = "${local.vpc_id}"

  subnets = [
    "${local.private_subnets}",
  ]

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"

  container_port = "${module.grafana_task.container_port}"
  container_name = "${module.grafana_task.container_name}"

  task_definition_arn = "${module.grafana_task.task_definition_arn}"

  healthcheck_path = "/api/health"

  launch_type = "EC2"
}

# Load balancer

resource "aws_alb" "public_services" {
  # This name can only contain alphanumerics and hyphens
  name = "${replace("${local.namespace}", "_", "-")}-v2"

  subnets = ["${local.public_subnets}"]

  security_groups = [
    "${aws_security_group.service_lb_security_group.id}",
    "${aws_security_group.external_lb_security_group.id}",
  ]
}

resource "aws_alb_listener" "https" {
  load_balancer_arn = "${aws_alb.public_services.id}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.certificate.arn}"

  default_action {
    target_group_arn = "${module.grafana_service.target_group_arn}"
    type             = "forward"
  }
}

data "aws_acm_certificate" "certificate" {
  domain   = "monitoring.wellcomecollection.org"
  statuses = ["ISSUED"]
}

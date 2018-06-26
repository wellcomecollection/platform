locals {
  namespace = "monitoring"
  vpc_cidr_block = "22.0.0.0/16"
}

# Network

module "network" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=ecs_v2"
  name       = "${local.namespace}"
  cidr_block = "${local.vpc_cidr_block}"
  az_count   = "2"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${local.namespace}"
  vpc  = "${module.network.vpc_id}"
}

resource "aws_ecs_cluster" "monitoring" {
  name = "monitoring_cluster"
}


# EC2

module "ec2_bastion" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/bastion?ref=ecs_v2"

  vpc_id = "${module.network.vpc_id}"

  name = "${local.namespace}-bastion-host"

  controlled_access_cidr_ingress = ["${var.admin_cidr_ingress}"]

  key_name    = "wellcomedigitalplatform"
  subnet_list = "${module.network.public_subnets}"

  asg_min     = "0"
  asg_desired = "0"
}

module "ec2_efs_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/ec2/prebuilt/efs?ref=ecs_v2"

  cluster_name = "${aws_ecs_cluster.monitoring.name}"
  vpc_id       = "${module.network.vpc_id}"

  asg_name = "${local.namespace}-v2"

  ssh_ingress_security_groups = ["${module.ec2_bastion.ssh_controlled_ingress_sg}"]
  custom_security_groups      = ["${aws_security_group.efs_security_group.id}"]

  subnets  = "${module.network.private_subnets}"
  key_name = "wellcomedigitalplatform"

  efs_fs_id = "${module.grafana_efs.efs_id}"
  region    = "${var.aws_region}"
}

# EFS

module "grafana_efs" {
  name                         = "grafana"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${module.network.vpc_id}"
  subnets                      = "${module.network.private_subnets}"
  efs_access_security_group_id = "${aws_security_group.efs_security_group.id}"
}

# IAM

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "${local.namespace}_cloudwatch_read"

  role   = "${module.grafana_task.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

data "aws_iam_policy_document" "allow_cloudwatch_read_metrics" {
  statement {
    actions = [
      "cloudwatch:DescribeAlarmHistory",
      "cloudwatch:DescribeAlarms",
      "cloudwatch:DescribeAlarmsForMetric",
      "cloudwatch:GetMetricData",
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:ListMetrics",
    ]

    resources = [
      "*",
    ]
  }
}

# ECS

module "grafana_task" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/task/prebuilt/efs?ref=ecs_v2"

  aws_region = "${var.aws_region}"
  task_name  = "${local.namespace}_ec2_private_efs"

  container_image = "grafana/grafana:4.4.3"
  container_port  = "3000"

  efs_host_path      = "${module.ec2_efs_host.efs_host_path}"
  efs_container_path = "/var/lib/grafana"

  cpu    = 256
  memory = 512

  env_vars = {
    GF_AUTH_ANONYMOUS_ENABLED = "${var.grafana_anonymous_enabled}"
    GF_AUTH_ANONYMOUS_ORG_ROLE = "${var.grafana_anonymous_role}"
    GF_SECURITY_ADMIN_USER = "${var.grafana_admin_user}"
    GF_SECURITY_ADMIN_PASSWORD = "${var.grafana_admin_password}"
  }
}

module "grafana_service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/service/prebuilt/load_balanced?ref=ecs_v2"

  service_name = "grafana_v2"
  task_desired_count = "1"

  security_group_ids = [
    "${aws_security_group.service_lb_security_group.id}",
    "${aws_security_group.service_egress_security_group.id}"
  ]

  ecs_cluster_id = "${aws_ecs_cluster.monitoring.id}"

  vpc_id  = "${module.network.vpc_id}"
  subnets = [
    "${module.network.private_subnets}"
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

  subnets         = ["${module.network.public_subnets}"]
  security_groups = ["${aws_security_group.service_lb_security_group.id}", "${aws_security_group.external_lb_security_group.id}"]
}

resource "aws_alb_listener" "http_80" {
  load_balancer_arn = "${aws_alb.public_services.id}"
  port              = "80"
  protocol          = "HTTP"

  default_action {
    target_group_arn = "${module.grafana_service.target_group_arn}"
    type             = "forward"
  }
}

resource "aws_alb_listener_rule" "path_rule_80" {
  listener_arn = "${aws_alb_listener.http_80.arn}"

  action {
    type             = "forward"
    target_group_arn = "${module.grafana_service.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["/"]
  }
}

# Security groups

resource "aws_security_group" "service_egress_security_group" {
  name        = "${local.namespace}_service_egress_security_group"
  description = "Allow traffic between services"
  vpc_id      = "${module.network.vpc_id}"

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-egress"
  }
}

resource "aws_security_group" "service_lb_security_group" {
  name        = "${local.namespace}_service_lb_security_group"
  description = "Allow traffic between services and load balancer"
  vpc_id      = "${module.network.vpc_id}"

  ingress {
    protocol  = "tcp"
    from_port = 3000
    to_port   = 3000
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-service-lb"
  }
}

resource "aws_security_group" "external_lb_security_group" {
  name        = "${local.namespace}_external_lb_security_group"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${module.network.vpc_id}"

  ingress {
    protocol  = "tcp"
    from_port = 80
    to_port   = 80

    cidr_blocks = ["${var.admin_cidr_ingress}"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-external-lb"
  }
}

resource "aws_security_group" "efs_security_group" {
  name        = "${local.namespace}_efs_security_group"
  description = "Allow traffic between services and efs"
  vpc_id      = "${module.network.vpc_id}"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags {
    Name = "${local.namespace}-efs"
  }
}
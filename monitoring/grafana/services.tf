locals {
  vpc_cidr_block = "11.0.0.0/16"
  namespace      = "monitoring"
  aws_region     = "eu-west-1"
}

resource "aws_ecs_cluster" "monitoring" {
  name = "${local.namespace}"
}

module "network" {
  source     = "../../network"
  name       = "${local.namespace}"
  cidr_block = "${local.vpc_cidr_block}"
  az_count   = "2"
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "monitoring"
  vpc  = "${module.network.vpc_id}"
}

module "grafana_efs" {
  name                          = "grafana"
  source                        = "../../../terraform-modules/efs"
  vpc_id                        = "${var.vpc_id}"
  subnets                       = ["${var.private_subnets}"]
  efs_access_security_group_ids = ""
}

module "grafana_task" {
  source = "../../../terraform-modules/ecs/modules/task/prebuilt/efs"

  aws_region = ""
  task_name = ""
  container_image = "grafana/grafana:4.4.3"
  container_port  = "3000"

  env_vars = {
    GF_AUTH_ANONYMOUS_ENABLED  = "${var.grafana_anonymous_enabled}"
    GF_AUTH_ANONYMOUS_ORG_ROLE = "${var.grafana_anonymous_role}"
    GF_SECURITY_ADMIN_USER     = "${var.grafana_admin_user}"
    GF_SECURITY_ADMIN_PASSWORD = "${var.grafana_admin_password}"
  }

  efs_container_path = "/var/lib/grafana"
  efs_host_path      = "/efs"
}


module "grafana_service" {
  source = "../../../terraform-modules/ecs/modules/service/prebuilt/load_balanced"

  service_name        = "grafana"
  vpc_id              = "${var.vpc_id}"
  task_definition_arn = ""

  subnets = ["${var.private_subnets}"]

  container_port      = "3000"
  container_name      = "app"

  ecs_cluster_id      = "${var.cluster_id}"
  launch_type         = "EC2"

  task_desired_count  = 1
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

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "ecs_grafana_task_cloudwatch_read"

  role = "${module.grafana_task.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

module "grafana_ec2_host" {
  source = "../modules/ec2/prebuilt/efs"

  cluster_name = "${var.cluster_name}"
  vpc_id       = "${var.vpc_id}"

  asg_name = "ecsV2-efs"

  ssh_ingress_security_groups = ["${module.ec2_bastion.ssh_controlled_ingress_sg}"]
  custom_security_groups      = ["${aws_security_group.grafana_security_group.id}"]

  subnets  = "${var.private_subnets}"
  key_name = "wellcomedigitalplatform"

  efs_fs_id = "${module.grafana_efs.efs_id}"
  region    = "${var.aws_region}"
}

module "ec2_bastion" {
  source = "../../ec2/prebuilt/bastion"

  vpc_id = "${var.vpc_id}"
  name = "monitoring-bastion-host"

  controlled_access_cidr_ingress = ["195.143.129.128/25"]

  key_name    = "wellcomedigitalplatform"
  subnet_list = "${var.public_subnets}"
}


resource "aws_security_group" "grafana_security_group" {
  name        = "efs_security_group"
  description = "Allow traffic between grafana and efs"
  vpc_id      = "${var.vpc_id}"

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
    Name = "grafana-efs"
  }
}
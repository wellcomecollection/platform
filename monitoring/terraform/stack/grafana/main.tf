# EC2 ECS Host

module "ec2_efs_host" {
  source = "../../modules/efs_host"

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

locals {
  container_port = 3000
  container_name = "app"
}

module "task" {
  source = "github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/single_container?ref=efcfe187e57cb05083ca2f0898533f94f96e7b0b"

  task_name = var.namespace

  cpu    = 256
  memory = 512

  container_image = "grafana/grafana:${var.grafana_version}"

  aws_region = var.aws_region

  container_port = local.container_port
  container_name = local.container_name

  # You need to run as EC2 if you're using EFS volumes
  launch_type = "EC2"

  # If creating a new volume, note that the grafana folder on the volume
  # will be owned by the root user.  Grafana runs as user 472 so be sure to ssh
  # into the EC2 instance and change the owner of the directory to 472.
  efs_volume_name    = "efs"
  efs_host_path      = "${module.ec2_efs_host.efs_host_path}/grafana"

  mount_points = [
    {
      sourceVolume  = "efs"
      containerPath = "/var/lib/grafana"
    },
  ]

  env_vars = {
    GF_AUTH_ANONYMOUS_ENABLED  = var.grafana_anonymous_enabled
    GF_AUTH_ANONYMOUS_ORG_ROLE = var.grafana_anonymous_role
    GF_SECURITY_ADMIN_USER     = var.grafana_admin_user
    GF_SECURITY_ADMIN_PASSWORD = var.grafana_admin_password
  }
}

module "service" {
  source = "github.com/wellcomecollection/terraform-aws-ecs-service.git//service?ref=v1.4.0"

  service_name = var.namespace
  cluster_arn  = var.cluster_arn

  desired_task_count = 1

  task_definition_arn = module.task.arn

  namespace_id = var.namespace_id

  subnets = var.private_subnets

  security_group_ids = [
    aws_security_group.service_lb_security_group.id,
    aws_security_group.service_egress_security_group.id,
  ]

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200

  launch_type = "EC2"

  target_group_arn = aws_alb_target_group.ecs_service.arn

  container_name = local.container_name
  container_port = local.container_port
}

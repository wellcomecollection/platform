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

module "task" {
  source = "github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/single_container?ref=ca0c2dc9c3604a2776b9ba1fa6866ba5cf2b826b"

  task_name = var.namespace

  cpu    = 256
  memory = 512

  container_image = "grafana/grafana:${var.grafana_version}"

  aws_region = var.aws_region

  container_port  = "3000"

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

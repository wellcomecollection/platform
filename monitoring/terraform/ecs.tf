resource "aws_ecs_cluster" "monitoring" {
  name = "monitoring"
}

module "ec2_efs_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/ec2/prebuilt/efs?ref=v11.0.0"

  cluster_name = "${aws_ecs_cluster.monitoring.name}"
  vpc_id       = "${local.vpc_id}"

  asg_name = "${local.namespace}-v2"

  ssh_ingress_security_groups = []
  custom_security_groups      = ["${aws_security_group.efs_security_group.id}"]

  subnets  = "${local.private_subnets}"
  key_name = "wellcomedigitalplatform"

  efs_fs_id = "${module.grafana_efs.efs_id}"
  region    = "${var.aws_region}"
}

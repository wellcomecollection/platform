module "ec2_bastion" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/bastion?ref=v11.0.0"

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
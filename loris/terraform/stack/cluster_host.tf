module "cluster_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/ec2/prebuilt/ebs?ref=v17.0.0"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${var.vpc_id}"

  asg_name                    = "${var.namespace}"
  ssh_ingress_security_groups = []

  subnets  = "${var.private_subnets}"
  key_name = "${var.key_name}"

  instance_type = "${var.instance_type}"

  asg_min     = "${var.asg_min}"
  asg_desired = "${var.asg_desired}"
  asg_max     = "${var.asg_max}"

  ebs_size        = "${var.ebs_size}"
  ebs_volume_type = "${var.ebs_volume_type}"
}

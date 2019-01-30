module "cluster_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/ec2/prebuilt/ebs?ref=4a81e34"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${var.vpc_id}"

  asg_name = "${var.namespace}"

  subnets = "${var.private_subnets}"

  instance_type = "${var.instance_type}"

  asg_min     = "${var.asg_min}"
  asg_desired = "${var.asg_desired}"
  asg_max     = "${var.asg_max}"

  ebs_size        = "${var.ebs_size}"
  ebs_volume_type = "${var.ebs_volume_type}"
}

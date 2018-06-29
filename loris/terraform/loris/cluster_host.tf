module "cluster_host" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/ebs?ref=v11.0.0"

  cluster_name = "${aws_ecs_cluster.cluster.name}"
  vpc_id       = "${var.vpc_id}"

  asg_name                    = "${var.namespace}"
  ssh_ingress_security_groups = ["${var.ssh_controlled_ingress_sg}"]

  subnets  = "${var.private_subnets}"
  key_name = "${var.key_name}"
}

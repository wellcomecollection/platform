resource "aws_security_group" "efs_mnt_sg" {
  name        = "efs-mnt"
  description = "Allow traffic from instances"
  vpc_id      = "${module.vpc_tools.vpc_id}"

  ingress {
    from_port = 2049
    to_port   = 2049
    protocol  = "tcp"

    security_groups = [
      "${module.tools_cluster_asg.instance_sg_id}",
    ]
  }
}

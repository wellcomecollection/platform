resource "aws_efs_file_system" "efs" {
  creation_token = "${var.name}_efs"
}

resource "aws_efs_mount_target" "mount_target" {
  count = "${length(var.subnets)}"
  file_system_id = "${aws_efs_file_system.efs.id}"
  subnet_id      = "${var.subnets[count.index]}"
  security_groups = ["${aws_security_group.efs_mnt.id}"]
}

resource "aws_security_group" "efs_mnt" {
  description = "security groupt for efs mounts"
  vpc_id      = "${var.vpc_id}"
  name        = "${var.name}_efs_sg"

  ingress {
    protocol  = "tcp"
    from_port = 2049
    to_port   = 2049

    security_groups = [
      "${var.efs_access_security_group_id}"
    ]
  }
}

resource "aws_security_group_rule" "monitoring_out_efs" {
  type = "egress"
  from_port   = 2049
  to_port     = 2049
  protocol    = "tcp"
  security_group_id = "${var.efs_access_security_group_id}"
  source_security_group_id = "${aws_security_group.efs_mnt.id}"
}
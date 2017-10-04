resource "aws_efs_file_system" "efs" {
  creation_token   = "${var.name}_efs"
  performance_mode = "${var.performance_mode}"
}

resource "aws_efs_mount_target" "mount_target" {
  count           = "${length(var.subnets)}"
  file_system_id  = "${aws_efs_file_system.efs.id}"
  subnet_id       = "${var.subnets[count.index]}"
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

    security_groups = ["${var.efs_access_security_group_ids}"]
  }
}

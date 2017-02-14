resource "aws_efs_file_system" "jenkins" { }

resource "aws_efs_mount_target" "jenkins" {
  count           = "${var.az_count}"
  file_system_id  = "${aws_efs_file_system.jenkins.id}"
  subnet_id       = "${element(aws_subnet.tools.*.id, count.index)}"
  security_groups = ["${aws_security_group.efs_mnt_sg.id}"]
}

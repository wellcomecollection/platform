output "instance_security_group" {
  value = "${aws_security_group.instance_sg.id}"
}

output "launch_configuration" {
  value = "${aws_launch_configuration.platform.id}"
}

output "asg_name" {
  value = "${aws_autoscaling_group.platform_asg.id}"
}

output "elb_hostname" {
  value = "${aws_alb.main.dns_name}"
}

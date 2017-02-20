output "instance_security_group" {
  value = "${aws_security_group.instance_sg.id}"
}

output "launch_configuration" {
  value = "${aws_launch_configuration.platform.id}"
}

output "elb_hostname" {
  value = "${aws_alb.main.dns_name}"
}

output "tools_subnets" {
  value = "${join(",", aws_subnet.tools.*.id)}"
}

output "name" {
  value = "${element(concat(aws_launch_configuration.spot_launch_config.*.name, aws_launch_configuration.ondemand_launch_config.*.name, aws_launch_configuration.ebs_launch_config.*.name, aws_launch_configuration.ebs_io1_launch_config.*.name), 0)}"
}

output "instance_sg_id" {
  value = "${aws_security_group.instance_sg.id}"
}

output "loadbalancer_sg_https_id" {
  value = "${aws_security_group.https.id}"
}

output "loadbalancer_sg_http_id" {
  value = "${aws_security_group.http.id}"
}

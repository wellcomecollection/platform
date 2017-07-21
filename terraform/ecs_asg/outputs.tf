output "instance_sg_id" {
  value = "${aws_security_group.instance_sg.id}"
}

output "loadbalancer_sg_https_id" {
  value = "${aws_security_group.https.id}"
}

output "loadbalancer_sg_http_id" {
  value = "${aws_security_group.http.id}"
}

output "asg_name" {
  value = "${aws_cloudformation_stack.ecs_asg.outputs["AsgName"]}"
}

output "asg_desired" {
  value = "${var.asg_desired}"
}

output "asg_max" {
  value = "${var.asg_max}"
}

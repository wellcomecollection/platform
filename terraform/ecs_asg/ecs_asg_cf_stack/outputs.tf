output "asg_name" {
  value = "${aws_cloudformation_stack.ecs_asg.outputs["AsgName"]}"
}

output "asg_desired" {
  value = "${var.asg_desired}"
}

output "asg_max" {
  value = "${var.asg_max}"
}

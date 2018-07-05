output "service_lb_security_group_id" {
  value = "${aws_security_group.service_lb_security_group.id}"
}

output "target_group_arn" {
  value = "${module.service.target_group_arn}"
}

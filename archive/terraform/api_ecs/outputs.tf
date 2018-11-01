output "task_role_name" {
  value = "${module.api_ecs.task_role_name}"
}

output "alb_dns_name" {
  value = "${aws_alb.services.dns_name}"
}

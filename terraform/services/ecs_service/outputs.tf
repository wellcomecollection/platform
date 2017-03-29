output "target_group_arn" {
  value = "${aws_alb_target_group.ecs_service.arn}"
}

output "role_name" {
  value = "${aws_iam_role.ecs_service.name}"
}

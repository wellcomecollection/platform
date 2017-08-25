output "batch_service_role_arn" {
  value = "${aws_iam_role.batch_service.arn}"
}

output "ecs_instance_role_arn" {
  value = "${aws_iam_role.ecs_instance.arn}"
}

output "spot_fleet_role_arn" {
  value = "${aws_iam_role.spot_fleet.arn}"
}

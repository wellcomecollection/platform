output "batch_service_role_arn" {
  value = "${aws_iam_role.batch_service.arn}"
}

output "ecs_instance_role_arn" {
  value = "${aws_iam_role.ecs_instance.arn}"
}

output "ecs_instance_role_name" {
  value = "${aws_iam_role.ecs_instance.arn}"
}

output "ec2_instance_profile" {
  value = "${aws_iam_instance_profile.ecs_instance.name}"
}

output "spot_fleet_role_arn" {
  value = "${aws_iam_role.spot_fleet.arn}"
}

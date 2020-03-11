output "role_name" {
  value = "${aws_iam_role.role.name}"
}

output "name" {
  value = "${aws_iam_instance_profile.instance_profile.name}"
}

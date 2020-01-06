output "elasticcloud_access_id" {
  value = "${aws_iam_access_key.elasticcloud.id}"
}

output "elasticcloud_access_secret" {
  value = "${aws_iam_access_key.elasticcloud.encrypted_secret}"
}

output "elasticcloud_readonly_role_arn" {
  value = "${aws_iam_role.elasticcloud-snapshot-readonly.arn}"
}

output "elasticcloud_readonly_role_name" {
  value = "${aws_iam_role.elasticcloud-snapshot-readonly.name}"
}
output "user_id" {
  value = "${aws_iam_access_key.admin.id}"
}

output "user_secret" {
  value = "${aws_iam_access_key.admin.encrypted_secret}"
}

output "password" {
  value = "${aws_iam_user_login_profile.admin.encrypted_password}"
}

output "user_id" {
  value = "${aws_iam_access_key.client.id}"
}

output "user_secret" {
  value = "${aws_iam_access_key.client.encrypted_secret}"
}

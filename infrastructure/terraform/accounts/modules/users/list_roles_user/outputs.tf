output "user_id" {
  value = "${aws_iam_access_key.access_key.id}"
}

output "user_secret" {
  value = "${aws_iam_access_key.access_key.encrypted_secret}"
}

output "name" {
  value = aws_iam_user.user.name
}

output "arn" {
  value = "${aws_iam_user.user.arn}"
}

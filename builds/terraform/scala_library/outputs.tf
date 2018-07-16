output "travis_ci_id" {
  value = "${aws_iam_access_key.travis_ci.id}"
}

output "travis_ci_encrypted_secret" {
  value = "${aws_iam_access_key.travis_ci.secret}"
}

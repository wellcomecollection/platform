# Travis CI user
resource "aws_iam_user" "travis_ci_storage" {
  name = "travis_ci_storage"
}

resource "aws_iam_access_key" "travis_ci" {
  user = "${aws_iam_user.travis_ci_storage.name}"
}

resource "aws_iam_user_policy" "travis_ci" {
  user   = "${aws_iam_user.travis_ci_storage.name}"
  policy = "${data.aws_iam_policy_document.travis_permissions.json}"
}

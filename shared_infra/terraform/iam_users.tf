# Travis CI user
resource "aws_iam_user" "travis_ci" {
  name = "travis_ci"
}

resource "aws_iam_access_key" "travis_ci" {
  user = "${aws_iam_user.travis_ci.name}"
}

resource "aws_iam_user_policy" "travis_ci" {
  user   = "${aws_iam_user.travis_ci.name}"
  policy = "${data.aws_iam_policy_document.travis_permissions.json}"
}

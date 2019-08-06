# Largely a kludge to work around the fact that the original CI user was
# just called "travis_ci", but that doesn't work when you want to create
# more than one repo!
locals {
  name     = "travis_ci_${var.repo_name}"
  username = "${var.repo_name == "platform" ? "travis_ci" : local.name}"
}

resource "aws_iam_user" "travis_ci" {
  name = "${local.username}"
}

resource "aws_iam_access_key" "travis_ci" {
  user = "${aws_iam_user.travis_ci.name}"
}

resource "aws_iam_user_policy" "travis_ci" {
  user   = "${aws_iam_user.travis_ci.name}"
  policy = "${data.aws_iam_policy_document.travis_permissions.json}"
}


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

# Miro images sync user
resource "aws_iam_user" "miro_images_sync" {
  name = "miro_images_sync"
}

resource "aws_iam_access_key" "miro_images_sync" {
  user = "${aws_iam_user.miro_images_sync.name}"
}

resource "aws_iam_user_policy" "miro_images_sync" {
  user   = "${aws_iam_user.miro_images_sync.name}"
  policy = "${data.aws_iam_policy_document.miro_images_sync.json}"
}

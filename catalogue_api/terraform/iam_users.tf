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

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

# This user is for a third party to place METS files in an ingest bucket
resource "aws_iam_user" "mets_ingest_read_write" {
  name = "mets_ingest_read_write"
}

resource "aws_iam_access_key" "mets_ingest_read_write" {
  user = "${aws_iam_user.mets_ingest_read_write.name}"
}

resource "aws_iam_user_policy" "mets_ingest_read_write" {
  user   = "${aws_iam_user.mets_ingest_read_write.name}"
  policy = "${data.aws_iam_policy_document.s3_mets_ingest_bucket_read_write.json}"
}

resource "aws_iam_user_policy" "wellcomecollection_mets_ingest_read_write" {
  user   = "${aws_iam_user.mets_ingest_read_write.name}"
  policy = "${data.aws_iam_policy_document.s3_wellcomecollection_mets_ingest_bucket_read_write.json}"
}

resource "aws_iam_user" "r3store" {
  name = "r3store"
}

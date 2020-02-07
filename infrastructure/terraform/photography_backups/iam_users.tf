resource "aws_iam_user" "photography_backups" {
  name = "photography_backups"
  path = "/automated_systems/"
}

resource "aws_iam_access_key" "photography_backups" {
  user    = aws_iam_user.photography_backups.name
  pgp_key = "keybase:${local.keybase_username}"
}

resource "aws_iam_user_policy" "allow_backups_s3_access" {
  user   = aws_iam_user.photography_backups.name
  policy = data.aws_iam_policy_document.s3_backups_full_access.json
}

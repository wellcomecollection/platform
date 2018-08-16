resource "aws_iam_role_policy" "archive_asset_lookup_dynamo_permission" {
  role = "${module.lambda_archive_asset_lookup.role_name}"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:Query"
      ],
      "Resource": "${data.aws_dynamodb_table.storage_manifest.arn}"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:HeadObject",
        "s3:GetObject"
      ],
      "Resource": "${data.aws_s3_bucket.storage_manifests.arn}/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "${data.aws_s3_bucket.storage_manifests.arn}"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "lambda_archive_ingest_sns" {
  role   = "${module.lambda_archive_ingest.role_name}"
  policy = "${module.archivist_topic.publish_policy}"
}
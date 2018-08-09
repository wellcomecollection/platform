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
    }
  ]
}
EOF
}

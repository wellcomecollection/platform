output "table_stream_arn" {
  value = "${aws_dynamodb_table.table.stream_arn}"
}

output "table_name" {
  value = "${aws_dynamodb_table.table.name}"
}

output "bucket_name" {
  value = "${aws_s3_bucket.sierra_data.id}"
}

output "read_policy" {
  value = "${data.aws_iam_policy_document.read_policy.json}"
}

output "dynamodb_full_access_policy" {
  value = "${data.aws_iam_policy_document.dynamodb_full_access_policy.json}"
}

output "dynamodb_update_policy" {
  value = "${data.aws_iam_policy_document.dynamodb_update_policy.json}"
}

output "table_name" {
  value = "${local.table_name}"
}

output "bucket_name" {
  value = "${local.bucket_name}"
}

output "read_policy" {
  value = "${data.aws_iam_policy_document.read_policy.json}"
}

output "full_access_policy" {
  value = "${data.aws_iam_policy_document.full_access_policy.json}"
}

output "dynamodb_update_policy" {
  value = "${data.aws_iam_policy_document.dynamodb_update_policy.json}"
}

data "aws_dynamodb_table" "storage_manifest" {
  name = "${var.storage_manifest_dynamo_table_name}"
}

data "aws_dynamodb_table" "storage_manifest" {
  name = "${module.vhs_archive_manifest.table_name}"
}

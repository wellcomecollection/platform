data "aws_dynamodb_table" "vhs_table" {
  name = "${var.vhs_table_name}"
}

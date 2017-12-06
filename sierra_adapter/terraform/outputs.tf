output "sierradata_table_stream_arn" {
  value = "${aws_dynamodb_table.sierradata_table.stream_arn}"
}

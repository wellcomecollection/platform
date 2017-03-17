output "calm_stream_arn" {
  value = "${aws_dynamodb_table.calm-dynamodb-table.stream_arn}"
}

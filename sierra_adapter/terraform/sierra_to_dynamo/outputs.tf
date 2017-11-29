output "dynamo_stream_arn" {
  value = "${aws_dynamodb_table.sierra_table.stream_arn}"
}

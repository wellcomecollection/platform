output "calm_stream_arn" {
  value = "${aws_dynamodb_table.calm-dynamodb-table.stream_arn}"
}

output "ecr_nginx" {
  value = "${aws_ecr_repository.nginx.repository_url}"
}

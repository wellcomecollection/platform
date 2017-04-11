output "calm_db_arn" {
  value = "${aws_dynamodb_table.calm_table.arn}"
}

output "calm_stream_arn" {
  value = "${aws_dynamodb_table.calm_table.stream_arn}"
}

output "ecr_nginx" {
  value = "${aws_ecr_repository.nginx.repository_url}"
}

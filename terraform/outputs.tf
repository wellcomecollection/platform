output "ecr_nginx" {
  value = "${aws_ecr_repository.nginx.repository_url}"
}

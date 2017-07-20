output "ecr_nginx" {
  value = "${module.ecr_repository_nginx.repository_url}"
}

output "terraform_apply_topic" {
  value = "${module.terminal_failure_alarm.arn}"
}

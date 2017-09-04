output "ecr_nginx" {
  value = "${module.ecr_repository_nginx.repository_url}"
}

output "terraform_apply_topic" {
  value = "${module.terminal_failure_alarm.arn}"
}

output "mets_ingest_arn" {
  value = "${aws_s3_bucket.mets-ingest.arn}"
}

output "wellcomecollection_mets_ingest_arn" {
  value = "${aws_s3_bucket.wellcomecollection-mets-ingest.arn}"
}

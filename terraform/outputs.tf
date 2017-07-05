output "ecr_nginx" {
  value = "${module.ecr_repository_nginx.repository_url}"
}

output "miro_readonly_key_id" {
  value = "${aws_iam_access_key.miro_images_readonly.id}"
}

output "miro_readonly_key_secret" {
  value = "${aws_iam_access_key.miro_images_readonly.secret}"
}

output "mets_ingest_read_write_key_id" {
  value = "${aws_iam_access_key.mets_ingest_read_write.id}"
}

output "mets_ingest_read_write_key_secret" {
  value = "${aws_iam_access_key.mets_ingest_read_write.secret}"
}


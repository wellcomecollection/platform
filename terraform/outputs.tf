output "ecr_nginx" {
  value = "${module.ecr_repository_nginx.repository_url}"
}

output "ecr_api" {
  value = "${module.ecr_repository_api.repository_url}"
}

output "ecr_ingestor" {
  value = "${module.ecr_repository_ingestor.repository_url}"
}

output "ecr_transformer" {
  value = "${module.ecr_repository_transformer.repository_url}"
}

output "ecr_id_minter" {
  value = "${module.ecr_repository_id_minter.repository_url}"
}

output "ecr_reindexer" {
  value = "${module.ecr_repository_reindexer.repository_url}"
}

output "s3_infra" {
  value = "${aws_s3_bucket.infra.id}"
}

output "miro_readonly_key_id" {
  value = "${aws_iam_access_key.miro_images_readonly.id}"
}

output "miro_readonly_key_secret" {
  value = "${aws_iam_access_key.miro_images_readonly.secret}"
}

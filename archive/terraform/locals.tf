locals {
  namespace                          = "archive-storage"
  lambda_error_alarm_arn             = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn                      = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                             = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  public_subnets                     = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
  private_subnets                    = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  archive_bucket_name                = "wellcomecollection-assets-archive-storage"
  ingest_bucket_name                 = "wellcomecollection-assets-archive-ingest"
  archivist_container_image          = "${module.ecr_repository_archivist.repository_url}:${var.release_ids["archivist"]}"
  registrar_container_image          = "${module.ecr_repository_registrar.repository_url}:${var.release_ids["registrar"]}"
  api_ecs_container_image            = "${module.ecr_repository_archive_api.repository_url}:${var.release_ids["archive_api"]}"
  nginx_services_ecs_container_image = "${data.aws_ecr_repository.ecr_repository_nginx_services.repository_url}:${var.release_ids["nginx_services"]}"
}

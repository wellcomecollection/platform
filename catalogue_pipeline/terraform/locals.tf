locals {
  lambda_error_alarm_arn      = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn               = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                      = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets             = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  transformer_container_image = "${module.ecr_repository_transformer.repository_url}:${var.release_ids["transformer"]}"
  recorder_container_image    = "${module.ecr_repository_recorder.repository_url}:${var.release_ids["recorder"]}"
  matcher_container_image     = "${module.ecr_repository_matcher.repository_url}:${var.release_ids["matcher"]}"
  merger_container_image      = "${module.ecr_repository_merger.repository_url}:${var.release_ids["merger"]}"
  id_minter_container_image   = "${module.ecr_repository_id_minter.repository_url}:${var.release_ids["id_minter"]}"
  ingestor_container_image    = "${module.ecr_repository_ingestor.repository_url}:${var.release_ids["ingestor"]}"
}

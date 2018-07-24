locals {
  lambda_error_alarm_arn      = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn               = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                      = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets             = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  transformer_container_image = "${module.ecr_repository_transformer.repository_url}:6d56e907adf7da1bbf50f3d88d8cdeeb75c10aa6"
  recorder_container_image    = "${module.ecr_repository_recorder.repository_url}:864e37937e2134d8b122445550eb284cc1fc5849"
  matcher_container_image     = "${module.ecr_repository_matcher.repository_url}:4eba43696ebbbde93eb8fe6480c77dcf0cc023a7"
  merger_container_image      = "${module.ecr_repository_merger.repository_url}:864e37937e2134d8b122445550eb284cc1fc5849"
  id_minter_container_image   = "${module.ecr_repository_id_minter.repository_url}:864e37937e2134d8b122445550eb284cc1fc5849"
  ingestor_container_image    = "${module.ecr_repository_ingestor.repository_url}:38f10ca9fe8ca8cfb9545236c29911fd6fa10b9b"
}

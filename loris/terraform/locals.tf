locals {
  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_public_subnets}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  cloudfront_logs_bucket_domain_name = "${data.terraform_remote_state.shared_infra.cloudfront_logs_bucket_domain_name}"

  loris_release_uri       = "${data.aws_ssm_parameter.loris_release_uri.value}"
  nginx_loris_release_uri = "${data.aws_ssm_parameter.nginx_loris_release_uri.value}"
}

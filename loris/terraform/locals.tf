locals {
  vpc_id_new          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  public_subnets_new  = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_public_subnets}"
  private_subnets_new = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"

  bucket_alb_logs_id = "${data.terraform_remote_state.shared_infra.bucket_alb_logs_id}"

  cloudfront_logs_bucket_domain_name = "${data.terraform_remote_state.shared_infra.cloudfront_logs_bucket_domain_name}"
}

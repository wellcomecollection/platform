locals {
  vpc_id          = "${data.terraform_remote_state.shared_infra.storage_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.storage_vpc_delta_private_subnets}"

  storage_archive_bucket_name        = "wellcomecollection-storage-archive"
  storage_access_bucket_name         = "wellcomecollection-storage-access"
}

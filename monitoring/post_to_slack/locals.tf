locals {
  snapshots_bucket_arn = "${data.terraform_remote_state.catalogue_api.snapshots_bucket_arn}"
}

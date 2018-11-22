module "vhs_miro2" {
  source = "./vhs"
  name   = "miro-complete"

  bucket_name = "wellcomecollection-vhs-miro-complete"

  table_write_max_capacity = 750
  account_id               = "${data.aws_caller_identity.current.account_id}"
}

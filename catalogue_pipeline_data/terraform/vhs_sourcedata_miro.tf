module "vhs_miro" {
  source = "./vhs"
  name   = "sourcedata-miro"

  table_write_max_capacity = 750
  account_id               = "${data.aws_caller_identity.current.account_id}"
}

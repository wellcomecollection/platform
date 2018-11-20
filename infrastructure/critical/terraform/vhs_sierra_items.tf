module "vhs_sierra_items" {
  source     = "./vhs"
  name       = "sourcedata-sierra-items"
  account_id = "${data.aws_caller_identity.current.account_id}"
}

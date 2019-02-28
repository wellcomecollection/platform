module "vhs_calm_inferred_data" {
  source     = "./vhs"
  name       = "calm_inferred"
  account_id = "${data.aws_caller_identity.current.account_id}"
}

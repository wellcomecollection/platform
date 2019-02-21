module "vhs_calm_sourcedata" {
  source     = "./vhs"
  name       = "calm"
  account_id = "${data.aws_caller_identity.current.account_id}"
}

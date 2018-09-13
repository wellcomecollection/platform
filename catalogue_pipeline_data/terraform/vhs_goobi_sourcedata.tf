module "vhs_goobi_mets" {
  source     = "./vhs"
  name       = "goobi-mets"
  account_id = "${data.aws_caller_identity.current.account_id}"
}

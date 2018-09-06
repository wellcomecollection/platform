module "network" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v11.0.0"
  name       = "bagger-bnumbers-processing"
  cidr_block = "${var.vpc_cidr_block}"
  az_count   = "2"
}

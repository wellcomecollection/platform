module "catalogue_vpc" {
  source     = "network"
  name       = "catalogue"
  cidr_block = "10.100.0.0/16"
  az_count   = "3"
  aws_region = "${var.aws_region}"
}

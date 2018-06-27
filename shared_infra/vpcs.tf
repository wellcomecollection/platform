module "catalogue_vpc" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v11.0.0"
  name       = "catalogue"
  cidr_block = "10.100.0.0/16"
  az_count   = "3"
}
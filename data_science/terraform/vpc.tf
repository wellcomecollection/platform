module "vpc" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v1.0.0"
  cidr_block = "10.90.0.0/16"
  az_count   = "2"
  name       = "data_science"
}

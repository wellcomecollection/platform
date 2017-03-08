module "vpc_main" {
  source     = "./network"
  cidr_block = "10.30.0.0/16"
  az_count   = "2"
}

module "vpc_tools" {
  source     = "./network"
  cidr_block = "10.40.0.0/16"
  az_count   = "2"
}

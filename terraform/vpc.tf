module "vpc_services" {
  source     = "./network"
  cidr_block = "10.50.0.0/16"
  az_count   = "2"
}

module "vpc_api" {
  source     = "./network"
  cidr_block = "10.30.0.0/16"
  az_count   = "2"
}

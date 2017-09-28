module "vpc_services" {
  source     = "../terraform/network"
  cidr_block = "10.50.0.0/16"
  az_count   = "2"
  name       = "services"
}

module "vpc_api" {
  source     = "../terraform/network"
  cidr_block = "10.30.0.0/16"
  az_count   = "2"
  name       = "monitoring"
}

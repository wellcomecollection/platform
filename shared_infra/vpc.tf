module "vpc_services" {
  source     = "../terraform/network"
  cidr_block = "10.50.0.0/16"
  az_count   = "2"
  name       = "services"
}

module "vpc_monitoring" {
  source     = "../terraform/network"
  cidr_block = "10.40.0.0/16"
  az_count   = "2"
  name       = "monitoring"
}

module "vpc_api" {
  source     = "../terraform/network"
  cidr_block = "10.30.0.0/16"
  az_count   = "2"
  name       = "monitoring"
}

module "vpc_batch" {
  source     = "../terraform/network"
  cidr_block = "10.60.0.0/16"
  az_count   = "2"
  name       = "batch"

  map_public_ip = true
}

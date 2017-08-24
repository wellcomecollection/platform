module "vpc_batch" {
  source     = "./network"
  cidr_block = "10.60.0.0/16"
  az_count   = "2"
  name       = "batch"

  map_public_ip = true
}

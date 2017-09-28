module "vpc_monitoring" {
  source     = "../terraform/network"
  cidr_block = "10.40.0.0/16"
  az_count   = "2"
  name       = "monitoring"
}

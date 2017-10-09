module "vpc_services" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v1.0.0"
  cidr_block = "10.50.0.0/16"
  az_count   = "2"
  name       = "services"
}
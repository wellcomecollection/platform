module "vpc_data_api" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v1.0.0"
  cidr_block = "10.80.0.0/16"
  az_count   = "2"
  name       = "data_api"
}

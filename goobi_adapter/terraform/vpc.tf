module "vpc_goobi_adapter" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v1.0.0"
  cidr_block = "10.100.0.0/16"
  az_count   = "2"
  name       = "goobi_adapter"
}

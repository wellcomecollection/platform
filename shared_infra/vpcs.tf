module "catalogue_vpc" {
  source     = "git::https://github.com/wellcometrust/terraform.git//network?ref=v11.0.0"
  name       = "catalogue"
  cidr_block = "10.100.0.0/16"
  az_count   = "3"
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id       = "${module.catalogue_vpc.vpc_id}"
  service_name = "com.amazonaws.${var.aws_region}.s3"
}

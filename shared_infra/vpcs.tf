module "catalogue_vpc" {
  source     = "../terraform-modules/network"
  name       = "catalogue"
  cidr_block = "10.100.0.0/16"
  az_count   = "3"
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id       = "${module.catalogue_vpc.vpc_id}"
  service_name = "com.amazonaws.${var.aws_region}.s3"
}

resource "aws_vpc_endpoint_route_table_association" "private_s3" {
  count = "${length(module.catalogue_vpc.private_route_table_ids)}"

  vpc_endpoint_id = "${aws_vpc_endpoint.s3.id}"
  route_table_id  = "${module.catalogue_vpc.private_route_table_ids[count.index]}"
}

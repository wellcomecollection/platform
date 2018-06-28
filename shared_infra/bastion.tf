module "bastion" {
  source = "git::https://github.com/wellcometrust/terraform.git//ec2/prebuilt/bastion?ref=v11.0.0"

  vpc_id = "${module.catalogue_vpc.vpc_id}"

  name = "catalogue-vpc-bastion"

  controlled_access_cidr_ingress = ["${var.admin_cidr_ingress}"]

  key_name    = "${var.key_name}"
  subnet_list = "${module.catalogue_vpc.public_subnets}"
}

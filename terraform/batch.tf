module "aws_batch" {
  source = "./batch_compute"

  subnets = "${join(",", formatlist("\"%s\"", module.vpc_batch.subnets))}"
  key_name = "${var.key_name}"
  admin_cidr_ingress = "${var.admin_cidr_ingress}"
  vpc_id = "${module.vpc_batch.vpc_id}"
}
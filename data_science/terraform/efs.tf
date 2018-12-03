resource "aws_security_group" "efs_security_group" {
  name        = "${local.namespace}_efs_security_group"
  description = "Allow traffic between data science VMs and EFS"
  vpc_id      = "${local.vpc_id}"

  tags {
    Name = "${local.namespace}-efs"
  }
}

module "efs" {
  name                          = "${local.namespace}_notebooks_efs"
  source                        = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v16.1.8"
  vpc_id                        = "${local.vpc_id}"
  subnets                       = "${local.private_subnets}"
  num_subnets                   = "3"
  efs_access_security_group_ids = ["${aws_security_group.efs_security_group.id}"]
}

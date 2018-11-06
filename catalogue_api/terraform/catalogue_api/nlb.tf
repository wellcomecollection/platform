# Network Load Balancer

module "nlb" {
  source = "../../../../terraform-modules/load_balancer/network"

  namespace       = "${var.namespace}"
  private_subnets = ["${var.subnets}"]
}

# Target group & listener

data "aws_lb_target_group" "target_group" {
  name = "${module.catalogue_api.target_group_name}"
}

resource "aws_lb_listener" "tcp" {
  load_balancer_arn = "${module.nlb.arn}"
  port              = "80"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = "${data.aws_lb_target_group.target_group.arn}"
  }
}

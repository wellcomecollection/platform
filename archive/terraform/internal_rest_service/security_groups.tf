data "aws_subnet" "private" {
  count = "${length(var.private_subnets)}"
  id    = "${element(var.private_subnets, count.index)}"
}

resource "aws_security_group" "nginx_tcp_access_security_group" {
  name        = "archive_nlb_${var.service_name}_nginx_security_group"
  description = "Allow traffic between load balancer and internet"
  vpc_id      = "${var.vpc_id}"

  ingress {
    protocol    = "tcp"
    from_port   = "${var.nginx_container_port}"
    to_port     = "${var.nginx_container_port}"
    cidr_blocks = ["${data.aws_subnet.private.*.cidr_block}"]
  }
}
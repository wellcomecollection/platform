resource "aws_ecs_cluster" "cluster" {
  name = "${replace(var.namespace, "_", "-")}"
}

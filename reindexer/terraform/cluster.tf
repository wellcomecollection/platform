resource "aws_ecs_cluster" "cluster" {
  name = "${replace(var.ecs_cluster_name, "_", "-")}"
}

resource "aws_ecs_cluster" "loris" {
  name = "loris_cluster"
}

resource "aws_ecs_cluster" "loris_ebs" {
  name = "loris_cluster_ebs"
}

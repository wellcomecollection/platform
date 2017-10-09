resource "aws_ecs_cluster" "loris" {
  name = "loris_cluster"
}

resource "aws_ecs_cluster" "loris_ebs" {
  name = "loris_cluster_ebs"
}

resource "aws_ecs_cluster" "loris_m4" {
  name = "loris_cluster_m4"
}

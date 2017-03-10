resource "aws_ecs_cluster" "main" {
  name = "platform_cluster"
}

resource "aws_ecs_cluster" "tools" {
  name = "tools_cluster"
}

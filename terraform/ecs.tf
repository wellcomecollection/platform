resource "aws_ecs_cluster" "services" {
  name = "services_cluster"
}

resource "aws_ecs_cluster" "api" {
  name = "api_cluster"
}

resource "aws_ecs_cluster" "tools" {
  name = "tools_cluster"
}

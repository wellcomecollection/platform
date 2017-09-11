resource "aws_ecs_cluster" "services" {
  name = "services_cluster"
}

resource "aws_ecs_cluster" "monitoring" {
  name = "monitoring_cluster"
}

resource "aws_ecs_cluster" "api" {
  name = "api_cluster"
}

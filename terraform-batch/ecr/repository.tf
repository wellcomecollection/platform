resource "aws_ecr_repository" "repository" {
  name = "uk.ac.wellcome/${var.name}"

  lifecycle {
    prevent_destroy = true
  }
}

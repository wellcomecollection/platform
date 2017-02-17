resource "aws_ecr_repository" "platform" {
  name = "uk.ac.wellcome/platform"

  lifecycle {
    prevent_destroy = true
  }
}

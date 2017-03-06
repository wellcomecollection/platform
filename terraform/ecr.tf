# One repository per application (see http://stackoverflow.com/a/37543992 and https://github.com/docker/docker/blob/master/image/spec/v1.2.md)

resource "aws_ecr_repository" "api" {
  name = "uk.ac.wellcome/api"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "transform" {
  name = "uk.ac.wellcome/transform"

  lifecycle {
    prevent_destroy = true
  }
}

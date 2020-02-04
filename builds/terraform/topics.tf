resource "aws_sns_topic" "ecr_pushes" {
  name = "ecr_pushes"
}

resource "aws_sns_topic" "lambda_pushes" {
  name = "lambda_pushes"
}

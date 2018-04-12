resource "aws_dynamodb_table" "deployments" {
  name             = "deployments"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "deployment_id"
  range_key        = "service_arn"
  stream_enabled   = true
  stream_view_type = "NEW_IMAGE"

  attribute {
    name = "deployment_id"
    type = "S"
  }

  attribute {
    name = "service_arn"
    type = "S"
  }
}

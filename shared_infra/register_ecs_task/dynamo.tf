resource "aws_dynamodb_table" "requested_tasks" {
  name             = "requested_tasks"
  read_capacity    = 1
  write_capacity   = 1
  hash_key         = "task_definition_arn"
  range_key        = "task_arn"
  stream_enabled   = true
  stream_view_type = "NEW_IMAGE"

  attribute {
    name = "task_definition_arn"
    type = "S"
  }

  attribute {
    name = "task_arn"
    type = "S"
  }
}

variable "stream_arn" {
  description = "ARN of the DynamoDB stream"
}

variable "function_arn" {
  description = "ARN of the AWS Lambda function to trigger"
}

variable "function_role" {
  description = "Name of the IAM role for the AWS Lambda"
}

variable "batch_size" {
  description = "Maximum batch size to retrieve from the stream"
  default     = 1
}

variable "starting_position" {
  description = "Position in the stream where AWS Lambda should start reading"
  default     = "LATEST"
}

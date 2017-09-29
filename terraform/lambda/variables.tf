variable "name" {
  description = "Name of the Lambda"
}

variable "description" {
  description = "Description of the Lambda function"
}

variable "environment_variables" {
  description = "Environment variables to pass to the Lambda"
  type        = "map"

  # environment cannot be empty so we need to pass at least one value
  default = {
    EMPTY_VARIABLE = ""
  }
}

variable "timeout" {
  description = "The amount of time your Lambda function has to run in seconds"
  default     = 3
}

variable "alarm_topic_arn" {
  description = "ARN of the topic where to send notification for lambda errors"
}

variable "s3_bucket" {
  description = "The S3 bucket containing the function's deployment package"
  default     = "platform-infra"
}

variable "s3_key" {
  description = "The S3 key of the function's deployment package"
}

variable "memory_size" {
  default = 128
}

variable "lambda_function_name" {
  description = "Name of the Lambda function to be triggered"
}

variable "lambda_function_arn" {
  description = "ARN of the Lambda function to be triggered"
}

variable "s3_bucket_arn" {
  description = "ARN of the S3 bucket containing the trigger file"
}

variable "s3_bucket_id" {
  description = "ID of the S3 bucket containing the trigger file"
}

variable "filter_prefix" {
  description = "(Optional) Specifies object key name prefix."
  default     = ""
}

variable "filter_suffix" {
  description = "(Optional) Specifies object key name suffix."
  default     = ""
}

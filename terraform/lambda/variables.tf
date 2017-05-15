variable "name" {
  description = "Name of the Lambda"
}

variable "description" {
  description = "Description of the Lambda function"
}

variable "source_dir" {
  description = "Path to the directory containing the Lambda source code"
}

variable "environment_variables" {
  description = "Environment variables to pass to the Lambda"
  type        = "map"

  # environment cannot be emtpy so we need to pass at least one value
  default     = {EMPTY_VARIABLE = ""}
}

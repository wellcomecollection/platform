variable "name" {}

variable "src_stream_arn" {}
variable "dst_topic_arn" {}

variable "batch_size" {
  default = 50
}

variable "stream_view_type" {
  default = "FULL_EVENT"
}

variable "lambda_error_alarm_arn" {}

variable "infra_bucket" {}

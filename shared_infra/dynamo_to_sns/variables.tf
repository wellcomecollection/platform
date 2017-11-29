variable "name" {}

variable "src_stream_arn" {}
variable "dst_topic_arn" {}
variable "batch_size" {
  default = 50
}

variable "lambda_error_alarm_arn" {}

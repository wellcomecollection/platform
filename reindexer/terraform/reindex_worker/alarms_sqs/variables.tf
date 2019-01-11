variable "name" {}
variable "queue_name" {}

variable "scale_up_arn" {}
variable "scale_down_arn" {}

variable "high_period_in_minutes" {}

variable "high_threshold" {
  default = 1
}

variable "low_period_in_minutes" {}

variable "low_threshold" {
  default = 1
}

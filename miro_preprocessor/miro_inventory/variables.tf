variable "lambda_error_alarm_arn" {}
variable "cluster_url" {}
variable "es_username" {}
variable "es_passsword" {}

variable "lambda_trigger_topic_arns" {
  type = "list"
}

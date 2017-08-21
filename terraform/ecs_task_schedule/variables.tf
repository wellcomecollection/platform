variable "cloudwatch_event_rule_name" {
  description = "Name of the event rule which triggers this task"
}

variable "cluster_arn" {
  description = "ARN of the cluster in which to run this task"
}

variable "task_definition_arn" {
  description = "ARN of the task definition to run"
}

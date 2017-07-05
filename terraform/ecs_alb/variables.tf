variable "name" {
  description = "Name of ALB"
}

variable "subnets" {
  type        = "list"
  description = "subnets to which ALB will route"
}

variable "loadbalancer_security_groups" {
  type        = "list"
  description = "Load balancer security group ID"
}

variable "certificate_domain" {
  description = "Domain name of the associated ACM certificate"
}

variable "health_check_path" {
  default = "/management/healthcheck"
}

variable "vpc_id" {
  description = "ID of VPC to create ALB in"
}

variable "alarm_topic_arn" {
  description = "ARN of the topic where to send notification for bad ALB state"
}
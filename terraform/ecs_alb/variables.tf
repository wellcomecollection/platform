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

variable "listener_port" {
  default     = "443"
  description = "Port for listener"
}

variable "listener_protocol" {
  default     = "HTTPS"
  description = "Protocol for listener"
}

variable "certificate_arn" {
  default     = ""
  description = "ARN of cert to use for HTTPS (if specified)"
}

variable "target_group_port" {
  default = "80"
}

variable "health_check_path" {
  default = "/management/healthcheck"
}

variable "vpc_id" {
  description = "ID of VPC to create ALB in"
}

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

variable "alb_access_log_bucket" {
  description = "S3 Bucket into which to place access log"
}

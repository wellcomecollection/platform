variable "name" {
  default = "batch_compute"
}

variable "min_vcpus" {
  default = 0
}

variable "max_vcpus" {
  default = 6
}

variable "desired_vcpus" {
  default = 3
}

variable "subnets" {}

variable "ec2_key_pair" {}

variable "bid_percentage" {
  default = 50
}

variable "vpc_id" {}
variable "admin_cidr_ingress" {}

variable "service_role" {}
variable "ecs_instance_role" {}
variable "spot_fleet_role" {}
variable "image_id" {}

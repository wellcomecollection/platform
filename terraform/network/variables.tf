variable "cidr_block" {
  description = "Name of the ECS service to create"
}

variable "az_count" {
  description = "Number of AZs to use"
}

variable "name" {
  description = "Name to use on resource tags"
}

variable "map_public_ip" {
  description = "Assign public IP addresses to instances launched in this subnet"
  default     = false
}

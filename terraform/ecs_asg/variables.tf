variable "asg_name" {
  description = "Name of the ASG to create"
}

variable "asg_min" {
  description = "Minimum number of instances"
  default     = "1"
}

variable "asg_desired" {
  description = "Desired number of instances"
  default     = "1"
}

variable "asg_max" {
  description = "Max number of instances"
  default     = "2"
}

variable "subnet_list" {
  type = "list"
}

variable "instance_type" {
  default     = "t2.small"
  description = "AWS instance type"
}

variable "key_name" {
  description = "SSH key pair name for instance sign-in"
}

variable "instance_profile_name" {
  description = "Instance profile for ec2 container hosts"
}

variable "user_data" {
  description = "User data for ec2 container hosts"
}

variable "public_ip" {
  description = "Associate public IP address?"
  default     = true
}

variable "vpc_id" {
  description = "VPC for EC2 autoscaling group security group"
}

variable "admin_cidr_ingress" {
  default     = "0.0.0.0/0"
  description = "CIDR for SSH access to EC2 instances"
}

variable "sns_topic_arn" {
  description = "ARN of the topic where to push notifications when an EC2 instance is set to terminating state"
}

variable "publish_to_sns_policy" {
  description = "Policy document to give permission to publish to the sns topic"
}

variable "alarm_topic_arn" {
  description = "ARN of the topic where to send notification for DLQs not being empty"
}

variable "image_id" {
  description = "ID of the AMI to use on the instances"
}

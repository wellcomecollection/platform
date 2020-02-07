variable "name" {}

variable "principals" {
  type = list
}

variable "max_session_duration_in_seconds" {
  # Default is one hour
  default = "3600"
}

variable "prefix" {}

variable "principals" {
  type = list(string)
}

variable "max_session_duration_in_seconds" {
  # Default is one hour
  default = "3600"
}

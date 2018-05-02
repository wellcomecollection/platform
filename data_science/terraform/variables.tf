variable "aws_region" {
  default = "eu-west-1"
}

variable "hashed_password" {
  # The default password is 'password'
  # To generate a new password run the following Python code:
  #
  # from notebook.auth import passwd
  #   passwd()
  #
  default = "sha1:5310f21e370d:a4d66e725c179218638c21c03d83933aa066db2d"
}

variable "key_name" {}

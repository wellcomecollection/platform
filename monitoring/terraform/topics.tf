module "load_test_results" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "load_test_results"
}

module "load_test_failure_alarm" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "load_test_failure_alarm"
}

module "load_test_results" {
  source = "../terraform/sns"
  name   = "load_test_results"
}

module "load_test_failure_alarm" {
  source = "../terraform/sns"
  name   = "load_test_failure_alarm"
}

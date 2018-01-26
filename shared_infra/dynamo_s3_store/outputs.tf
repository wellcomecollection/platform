output "read_policy" {
  value = "${data.iam_policy_document.read_policy.json}"
}

output "full_access_policy" {
  value = "${data.iam_policy_document.full_access_policy.json}"
}

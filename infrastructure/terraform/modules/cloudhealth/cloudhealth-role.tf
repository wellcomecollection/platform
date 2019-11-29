resource "aws_iam_role" "role" {
  assume_role_policy   = "${file("${path.module}/policies/wt-cloudhealth-role.json")}"
  description          = "This role provides read only access for cloudhealth"
  name                 = "cloudhealth"
}

resource "aws_iam_policy" "policy" {
  policy      = "${file("${path.module}/policies/wt-cloudhealth-policy.json")}"
  description = "This policy allows access to billing for cloudhealth - read only"
  name        = "cloudhealth"
}

resource "aws_iam_policy_attachment" "wt-cloudhealth-policy-attachement" {
  name       = "cloudhealth"
  policy_arn = "${aws_iam_policy.policy.arn}"
  roles      = ["${aws_iam_role.role.name}"]
}

output "role_arn" {
  value = "${aws_iam_role.role.arn}"
}
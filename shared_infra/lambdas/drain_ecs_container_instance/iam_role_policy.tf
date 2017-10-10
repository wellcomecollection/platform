resource "aws_iam_role_policy" "drain_ecs_container_instance_sns" {
  name   = "drain_ecs_container_instance_publish_to_sns"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${var.ec2_terminating_topic_publish_policy}"
}

resource "aws_iam_role_policy" "drain_ecs_container_instance_asg_complete" {
  name   = "drain_ecs_container_instance_asg_complete"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.complete_lifecycle_hook.json}"
}

resource "aws_iam_role_policy" "drain_ecs_container_instance_ecs_list" {
  name   = "drain_ecs_container_instance_ecs_list"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.ecs_list_container_tasks.json}"
}

resource "aws_iam_role_policy" "drain_ecs_container_instance_ec2_describe" {
  name   = "drain_ecs_container_instance_ec2_describe"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.ec2_describe_instances.json}"
}

resource "aws_iam_role_policy" "drain_ecs_container_send_asg_heartbeat" {
  name   = "drain_ecs_container_instance_send_asg_heartbeat"
  role   = "${module.lambda_drain_ecs_container_instance.role_name}"
  policy = "${data.aws_iam_policy_document.send_asg_heartbeat.json}"
}

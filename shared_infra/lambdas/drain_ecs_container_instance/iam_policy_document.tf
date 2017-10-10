data "aws_iam_policy_document" "complete_lifecycle_hook" {
  statement {
    actions = ["autoscaling:CompleteLifecycleAction"]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "ecs_list_container_tasks" {
  statement {
    actions = [
      "ecs:UpdateContainerInstancesState",
      "ecs:ListTasks",
      "ecs:DescribeContainerInstances",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "ec2_describe_instances" {
  statement {
    actions = [
      "ec2:DescribeInstances",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "send_asg_heartbeat" {
  statement {
    actions = [
      "autoscaling:RecordLifecycleActionHeartbeat",
    ]

    resources = [
      "*",
    ]
  }
}

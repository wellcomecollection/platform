module "ecs_services_iam" {
  source = "./ecs_iam"
  name   = "services"
}

module "ecs_api_iam" {
  source = "./ecs_iam"
  name   = "api"
}

module "ecs_tools_iam" {
  source = "./ecs_iam"
  name   = "tools"
}

resource "aws_iam_role_policy" "ecs_jenkins_task" {
  name = "ecs_task_jenkins_policy"
  role = "${module.ecs_tools_iam.task_role_name}"

  policy = "${data.aws_iam_policy_document.allow_everything.json}"
}

data "aws_iam_policy_document" "allow_everything" {
  statement {
    actions = [
      "*",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "ecs_ingestor_task" {
  name = "ecs_task_ingestor_policy"
  role = "${module.ingestor.role_name}"

  policy = "${data.aws_iam_policy_document.read_ingestor_q.json}"
}

data "aws_iam_policy_document" "read_ingestor_q" {
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "${module.ingest_queue.q_arn}",
    ]
  }
}

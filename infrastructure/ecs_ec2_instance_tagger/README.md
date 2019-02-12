#ecs_ec2_instance_tagger

Tag EC2 instances with their cluster ARN and container instance ARN.
This allows us to easily follow an instance back to its ECS cluster.

This script is triggered by an ECS Container Instance State Change event.

Requires a Cloudwatch Event with pattern:

{
  "source": [
    "aws.ecs"
  ],
  "detail-type": [
    "ECS Container Instance State Change"
  ]
}
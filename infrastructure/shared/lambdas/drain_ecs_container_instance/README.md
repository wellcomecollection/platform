#drain_ecs_container_instance

This task tries to ensure graceful termination of ECS container instances.

The SNS topic "ec2_terminating" receives messages telling us about terminating
EC2 instances. If the terminating instance is part of an ECS cluster, it
drains the ECS tasks on the instance.
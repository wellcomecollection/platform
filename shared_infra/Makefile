ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = shared_infra
SBT_DOCKER_LIBRARIES =
SBT_NO_DOCKER_LIBRARIES =

SBT_APPS    =
ECS_TASKS   = sqs_freezeray sqs_redrive
LAMBDAS     = drain_ecs_container_instance \
              dynamo_to_sns \
              ecs_ec2_instance_tagger \
              run_ecs_task

TF_NAME     = shared_infra
TF_PATH     = $(STACK_ROOT)
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))


shared_infra-test:	drain_ecs_container_instance-test \
					dynamo_to_sns-test \
					ecs_ec2_instance_tagger-test \
					run_ecs_task-test

shared_infra-publish:	drain_ecs_container_instance-publish \
						dynamo_to_sns-publish \
						ecs_ec2_instance_tagger-publish \
						run_ecs_task-publish

ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = shared_infra

SBT_APPS    =
ECS_TASKS   = sqs_freezeray sqs_redrive
LAMBDAS     = drain_ecs_container_instance \
				dynamo_to_sns \
				ecs_ec2_instance_tagger \
				run_ecs_task \
				service_scheduler \
				update_dynamo_capacity \
				update_ecs_service_size

TF_NAME     = shared_infra
TF_PATH     = $(ROOT)/$(STACK_ROOT)
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))



FREEZERAY = $(ROOT)/shared_infra/sqs_freezeray

$(FREEZERAY)/requirements.txt: $(FREEZERAY)/requirements.in
	docker run --rm --volume $(FREEZERAY):/src --rm micktwomey/pip-tools


SQS_REDRIVE = $(ROOT)/shared_infra/sqs_redrive

$(SQS_REDRIVE)/requirements.txt: $(SQS_REDRIVE)/requirements.in
	docker run --rm --volume $(SQS_REDRIVE):/src --rm micktwomey/pip-tools

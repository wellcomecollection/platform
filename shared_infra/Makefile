export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra

include $(ROOT)/functions.Makefile

include $(SHARED_INFRA)/drain_ecs_container_instance/Makefile
include $(SHARED_INFRA)/dynamo_to_sns/Makefile
include $(SHARED_INFRA)/ecs_ec2_instance_tagger/Makefile
include $(SHARED_INFRA)/run_ecs_task/Makefile
include $(SHARED_INFRA)/service_scheduler/Makefile
include $(SHARED_INFRA)/update_dynamo_capacity/Makefile
include $(SHARED_INFRA)/update_ecs_service_size/Makefile
include $(SHARED_INFRA)/sqs_freezeray/Makefile
include $(SHARED_INFRA)/sqs_redrive/Makefile

## Tests for shared_infra
shared_infra-test: drain_ecs_container_instance-test \
                   dynamo_to_sns-test \
                   ecs_ec2_instance_tagger-test \
                   run_ecs_task-test \
                   service_scheduler-test \
                   update_dynamo_capacity-test \
                   update_ecs_service_size-test \
				   sqs_freezeray-build\
				   sqs_redrive-build

## Build shared_infra
shared_infra-build: sqs_freezeray-build	\
                    sqs_redrive-build

## Publish shared_infra
shared_infra-deploy: drain_ecs_container_instance-publish \
                      dynamo_to_sns-publish \
                      ecs_ec2_instance_tagger-publish \
                      run_ecs_task-publish \
                      service_scheduler-publish \
                      update_dynamo_capacity-publish \
                      update_ecs_service_size-publish \
                      sqs_freezeray-publish	\
                      sqs_redrive-publish

## Run a plan on shared_infra stack
shared_infra-terraform-plan:
	$(call terraform_plan,$(SHARED_INFRA),false)

## Run an apply on shared_infra stack
shared_infra-terraform-apply:
	$(call terraform_apply,$(SHARED_INFRA))

export ROOT = $(shell git rev-parse --show-toplevel)
export SHARED_INFRA = $(ROOT)/shared_infra
export INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(SHARED_INFRA)/drain_ecs_container_instance/Makefile
include $(SHARED_INFRA)/dynamo_to_sns/Makefile
include $(SHARED_INFRA)/ecs_ec2_instance_tagger/Makefile
include $(SHARED_INFRA)/run_ecs_task/Makefile
include $(SHARED_INFRA)/register_ecs_task/Makefile
include $(SHARED_INFRA)/service_scheduler/Makefile
include $(SHARED_INFRA)/update_dynamo_capacity/Makefile
include $(SHARED_INFRA)/update_ecs_service_size/Makefile
include $(SHARED_INFRA)/update_task_for_config_change/Makefile
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
                   update_task_for_config_change-test

## Build shared_infra
shared_infra-build: drain_ecs_container_instance-build \
                    dynamo_to_sns-build \
                    ecs_ec2_instance_tagger-build \
                    run_ecs_task-build \
                    service_scheduler-build \
                    update_dynamo_capacity-build \
                    update_ecs_service_size-build \
                    update_task_for_config_change-build \
                    sqs_freezeray-build	\
                    sqs_redrive-build

## Publish shared_infra
shared_infra-deploy: drain_ecs_container_instance-publish \
                      dynamo_to_sns-publish \
                      ecs_ec2_instance_tagger-publish \
                      run_ecs_task-publish \
                      service_scheduler-publish \
                      update_dynamo_capacity-publish \
                      update_ecs_service_size-publish \
                      update_task_for_config_change-publish \
                      sqs_freezeray-publish	\
                      sqs_redrive-publish

## Run a plan on shared_infra stack
shared_infra-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(SHARED_INFRA):/data \
	-e OP=plan \
	terraform_ci:latest

## Run an apply on shared_infra stack
shared_infra-terraform-apply: $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
	-v $(SHARED_INFRA):/data \
	-e OP=apply \
	terraform_ci:latest


.PHONY: shared_infra-test shared_infra-build shared_infra-deploy shared_infra-terraform-plan shared_infra-terraform-apply
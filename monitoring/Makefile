ROOT = $(shell git rev-parse --show-toplevel)
MONITORING = $(ROOT)/monitoring
INFRA_BUCKET = platform-infra

include $(MONITORING)/deployment_tracking/Makefile
include $(MONITORING)/ecs_dashboard/Makefile
include $(MONITORING)/load_test/Makefile

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile

## Build post_to_slack lambda
post_to_slack-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(MONITORING)/post_to_slack:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test post_to_slack lambda
post_to_slack-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/post_to_slack/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish post_to_slack lambda
post_to_slack-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "monitoring/post_to_slack/src" \
		--key="lambdas/monitoring/post_to_slack.zip" \
		--bucket="$(INFRA_BUCKET)"; \

## Test monitoring
monitoring-test: post_to_slack-test

## Publish monitoring
monitoring-deploy: post_to_slack-publish

## Terraform plan on monitoring stack
monitoring-terraform-plan:
	$(call terraform_plan,$(MONITORING))

## Terraform apply on monitoring stack
monitoring-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING):/data \
		--env OP=apply \
		terraform_ci:latest


.PHONY: post_to_slack-build post_to_slack-test post_to_slack-publish \
		monitoring-terraform-plan monitoring-terraform-apply \
		monitoring-test monitoring-deploy
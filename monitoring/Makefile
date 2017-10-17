ROOT = $(shell git rev-parse --show-toplevel)
MONITORING = $(ROOT)/monitoring
INFRA_BUCKET = platform-infra

include $(MONITORING)/deployment_tracking/Makefile
include $(MONITORING)/ecs_dashboard/Makefile
include $(MONITORING)/load_test/Makefile

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

post_to_slack-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(MONITORING)/post_to_slack:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

post_to_slack-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/post_to_slack/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

post_to_slack-publish: $(ROOT)/.docker/publish_lambda_zip \
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "post_to_slack/src" \
		--key="lambdas/monitoring/post_to_slack.zip" \
		--bucket="$(INFRA_BUCKET)"; \

monitoring-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/terraform:/data \
		--env OP=plan \
		terraform_ci:latest

monitoring-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/terraform:/data \
		--env OP=apply \
		terraform_ci:latest


.PHONY: post_to_slack-build post_to_slack-test post_to_slack-publish monitoring-terraform-plan monitoring-terraform-apply
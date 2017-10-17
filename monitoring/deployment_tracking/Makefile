ROOT = $(shell git rev-parse --show-toplevel)
DEPLOYMENT_TRACKING = $(ROOT)/monitoring/deployment_tracking
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


notify_old_deploys-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

notify_old_deploys-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

notify_old_deploys-publish: $(ROOT)/.docker/publish_lambda_zip \
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "notify_old_deploys/src" \
		--key="lambdas/ecs_dashboard/notify_old_deploys.zip" \
		--bucket="$(INFRA_BUCKET)"; \

service_deployment_status-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

service_deployment_status-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(DEPLOYMENT_TRACKING)/notify_old_deploys/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

service_deployment_status-publish: $(ROOT)/.docker/publish_lambda_zip \
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "notify_old_deploys/src" \
		--key="lambdas/ecs_dashboard/notify_old_deploys.zip" \
		--bucket="$(INFRA_BUCKET)"; \

deployment_tracking-build: notify_old_deploys-build service_deployment_status-build
deployment_tracking-test: notify_old_deploys-test service_deployment_status-test
deployment_tracking-publish: notify_old_deploys-publish service_deployment_status-publish

.PHONY: notify_old_deploys-build notify_old_deploys-test notify_old_deploys-publish \
		service_deployment_status-build service_deployment_status-test service_deployment_status-publish \
		deployment_tracking-build deployment_tracking-test deployment_tracking-publish


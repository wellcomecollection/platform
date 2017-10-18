ROOT = $(shell git rev-parse --show-toplevel)
ECS_DASHBOARD = $(ROOT)/monitoring/ecs_dashboard
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


$(ROOT)/.docker/ecs_dashboard:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=ecs_dashboard \
		--dir=$(ECS_DASHBOARD)/client \
		--file=$(ECS_DASHBOARD)/client/Dockerfile

## Publish ecs_dashboard client to S3
ecs_dashboard-deploy: $(ROOT)/.docker/ecs_dashboard
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/client:/dashboard \
		--env BUCKET_NAME=wellcome-platform-dash \
		--env PATH_PREFIX=https://s3-eu-west-1.amazonaws.com/wellcome-platform-dash/ \
		ecs_dashboard

## Build update_service_list lambda
update_service_list-build: $(ROOT)/.docker/python3.6_ci
	docker run \
		--volume $(ECS_DASHBOARD)/update_service_list:/data \
		--env OP=build-lambda \
		python3.6_ci:latest

## Test update_service_list lambda
update_service_list-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/update_service_list/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish update_service_list lambda
update_service_list-publish: $(ROOT)/.docker/publish_lambda_zip
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ROOT):/repo \
		publish_lambda_zip "monitoring/ecs_dashboard/update_service_list/src" \
		--key="lambdas/ecs_dashboard/update_service_list.zip" \
		--bucket="$(INFRA_BUCKET)"; \

## Build all for ecs_dashboard
ecs_dashboard-build: update_service_list-build

## Test all for ecs_dashboard
ecs_dashboard-test: update_service_list-test

## Publish all for ecs_dashboard
ecs_dashboard-publish: update_service_list-publish

.PHONY: ecs_dashboard-deploy update_service_list-build update_service_list-test update_service_list-publish \
		ecs_dashboard-build ecs_dashboard-test ecs_dashboard-publish

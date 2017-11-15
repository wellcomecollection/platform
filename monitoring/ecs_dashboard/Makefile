ROOT = $(shell git rev-parse --show-toplevel)
ECS_DASHBOARD = $(ROOT)/monitoring/ecs_dashboard
INFRA_BUCKET = platform-infra

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif

include $(ROOT)/functions.Makefile


$(ROOT)/.docker/ecs_dashboard:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=ecs_dashboard \
		--dir=$(ECS_DASHBOARD)/client \
		--file=$(ECS_DASHBOARD)/client/Dockerfile

## Publish ecs_dashboard client to S3
ecs_dashboard_client-deploy: $(ROOT)/.docker/ecs_dashboard
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/client:/dashboard \
		--env BUCKET_NAME=wellcome-platform-dash \
		--env PATH_PREFIX=https://s3-eu-west-1.amazonaws.com/wellcome-platform-dash/ \
		ecs_dashboard

## Test update_service_list lambda
update_service_list-test: $(ROOT)/.docker/python3.6_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/update_service_list/src:/data \
		--env OP=test \
		--env FIND_MATCH_PATHS="/data" --tty \
		python3.6_ci:latest

## Publish update_service_list lambda
update_service_list-publish:
	$(call publish_lambda,monitoring/ecs_dashboard/update_service_list)

## Build all for ecs_dashboard
ecs_dashboard-build:
	echo "Nothing to do!"

## Test all for ecs_dashboard
ecs_dashboard-test: update_service_list-test

## Publish all for ecs_dashboard
ecs_dashboard-deploy: update_service_list-publish

.PHONY: ecs_dashboard-deploy update_service_list-test update_service_list-publish \
		ecs_dashboard-build ecs_dashboard-test ecs_dashboard_client-deploy

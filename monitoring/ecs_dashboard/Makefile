ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring/ecs_dashboard

SBT_APPS    =
ECS_TASKS   =
LAMBDAS     = update_service_list

TF_NAME     =
TF_PATH     =
TF_IS_PUBLIC_FACING =

$(val $(call stack_setup))


ECS_DASHBOARD = $(ROOT)/monitoring/ecs_dashboard

build_deployment_container:
	docker build \
		-f client/Dockerfile client/ \
		--tag ecs_dashboard

ecs_dashboard_client-publish: build_deployment_container
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/client:/dashboard \
		--env BUCKET_NAME=wellcomecollection-platform-dashboard \
		--env PATH_PREFIX=https://wellcomecollection-platform-dashboard.s3.amazonaws.com \
		ecs_dashboard

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

$(ROOT)/.docker/ecs_dashboard:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=ecs_dashboard \
		--dir=$(ECS_DASHBOARD)/client \
		--file=$(ECS_DASHBOARD)/client/Dockerfile

ecs_dashboard_client-publish: $(ROOT)/.docker/ecs_dashboard
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/client:/dashboard \
		--env BUCKET_NAME=wellcomecollection-platform-dashboard \
		--env PATH_PREFIX=https://s3-eu-west-1.amazonaws.com/wellcomecollection-platform-dashboard/ \
		ecs_dashboard

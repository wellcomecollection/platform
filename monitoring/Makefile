ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

STACK_ROOT  = monitoring

SBT_APPS    =
ECS_TASKS   = gatling
LAMBDAS     = gatling_to_cloudwatch \
              notify_old_deploys \
              post_to_slack \
              service_deployment_status \
              terraform_tracker \
              update_service_list

TF_NAME     = monitoring
TF_PATH     = $(ROOT)/$(STACK_ROOT)/terraform
TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))


ECS_DASHBOARD = $(ROOT)/monitoring/ecs_dashboard

$(ROOT)/.docker/ecs_dashboard:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=ecs_dashboard \
		--dir=$(ECS_DASHBOARD)/client \
		--file=$(ECS_DASHBOARD)/client/Dockerfile

ecs_dashboard_client-publish: $(ROOT)/.docker/ecs_dashboard
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(ECS_DASHBOARD)/client:/dashboard \
		--env BUCKET_NAME=wellcome-platform-dash \
		--env PATH_PREFIX=https://s3-eu-west-1.amazonaws.com/wellcome-platform-dash/ \
		ecs_dashboard

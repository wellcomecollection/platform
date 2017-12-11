ROOT = $(shell git rev-parse --show-toplevel)
ECS_DASHBOARD = $(ROOT)/monitoring/ecs_dashboard

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


update_service_list-build:
	$(call build_lambda,monitoring/ecs_dashboard/update_service_list)

update_service_list-test:
	$(call test_lambda,monitoring/ecs_dashboard/update_service_list)

update_service_list-publish:
	$(call publish_lambda,monitoring/ecs_dashboard/update_service_list)


ecs_dashboard-build: update_service_list-build

ecs_dashboard-test: update_service_list-test

ecs_dashboard-deploy: update_service_list-publish

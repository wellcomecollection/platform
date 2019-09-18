ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT  = monitoring

PYTHON_APPS     = slack_budget_bot
PYTHON_SSM_APPS =
LAMBDAS 	    =   post_to_slack terraform_tracker \
                    update_service_list

$(val $(call stack_setup))

$(ROOT)/.docker/ecs_dashboard:
	docker build \
		-f client/Dockerfile client/ \
		--tag ecs_dashboard

	mkdir -p $(ROOT)/.docker; \
	touch $(ROOT)/.docker/ecs_dashboard

ecs_dashboard_client-publish: $(ROOT)/.docker/ecs_dashboard
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT)/ecs_dashboard:/dashboard \
		--env BUCKET_NAME=wellcomecollection-platform-dashboard \
		--env PATH_PREFIX=https://wellcomecollection-platform-dashboard.s3.amazonaws.com \
		ecs_dashboard

gatling_test:
	docker run -it \
		--volume $(ROOT)/monitoring/gatling/user-files:/opt/gatling/user-files \
		--volume $(ROOT)/monitoring/gatling/results:/opt/gatling/results \
		denvazh/gatling

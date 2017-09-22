ROOT = $(shell git rev-parse --show-toplevel)
MONITORING = $(ROOT)/monitoring

ifneq ($(ROOT), $(shell pwd))
	include $(ROOT)/shared.Makefile
endif


monitoring-terraform-plan: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/terraform:/data \
		--volume $(ROOT)/terraform:/terraform \
		--env OP=plan \
		terraform_ci:latest

monitoring-terraform-apply: uptodate-git $(ROOT)/.docker/terraform_ci
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/terraform:/data \
		--volume $(ROOT)/terraform:/terraform \
		--env OP=apply \
		terraform_ci:latest


$(ROOT)/.docker/ecs_dashboard:
	$(ROOT)/builds/build_ci_docker_image.py \
		--project=ecs_dashboard \
		--dir=$(MONITORING)/ecs_dashboard \
		--file=$(MONITORING)/ecs_dashboard/Dockerfile

ecs_dashboard-deploy: $(ROOT)/.docker/ecs_dashboard
	$(ROOT)/builds/docker_run.py --aws -- \
		--volume $(MONITORING)/ecs_dashboard:/dashboard \
		--env BUCKET_NAME=wellcome-platform-dash \
		--env PATH_PREFIX=https://s3-eu-west-1.amazonaws.com/wellcome-platform-dash/ \
		ecs_dashboard


gatling-build: $(ROOT)/.docker/image_builder
	./builds/docker_run.py --dind -- image_builder \
		--project=gatling \
		--file=monitoring/gatling/Dockerfile

gatling-deploy: gatling-build $(ROOT)/.docker/publish_service_to_aws
	PROJECT=gatling ./builds/publish_service.sh


.PHONY: monitoring-terraform-plan monitoring-terraform-apply \
	ecs_dashboard-deploy \
	gatling-build gatling-deploy

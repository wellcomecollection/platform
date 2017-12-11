export ROOT = $(shell git rev-parse --show-toplevel)

include $(ROOT)/functions.Makefile

ecs_ec2_instance_tagger-build:
	$(call build_lambda,shared_infra/ecs_ec2_instance_tagger)

ecs_ec2_instance_tagger-test:
	$(call test_lambda,shared_infra/ecs_ec2_instance_tagger)

ecs_ec2_instance_tagger-publish:
	$(call publish_lambda,shared_infra/ecs_ec2_instance_tagger)

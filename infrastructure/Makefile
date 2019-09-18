ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT = infrastructure

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    = ecs_ec2_instance_tagger

$(val $(call stack_setup))

infrastructure-test: ecs_ec2_instance_tagger-test
infrastructure-publish: ecs_ec2_instance_tagger-publish
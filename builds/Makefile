ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= builds

SBT_APPS 	 =
SBT_SSM_APPS =

ECR_BASE_URI =

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    = notify_pushes

TF_NAME = builds
TF_PATH = $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

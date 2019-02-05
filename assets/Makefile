ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	=

SBT_APPS 	 =
SBT_SSM_APPS =

ECR_BASE_URI =
REGISTRY_ID  =

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    =

TF_NAME 	= assets
TF_PATH 	= assets/terraform

TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))

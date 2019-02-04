ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= catalogue_api

SBT_APPS 	 =
SBT_SSM_APPS = api

ECR_BASE_URI = 760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS = update_api_docs
LAMBDAS 	    =

TF_NAME = catalogue_api
TF_PATH = $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))

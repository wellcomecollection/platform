ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT  = data_api

SBT_APPS 	 =
SBT_SSM_APPS = snapshot_generator

ECR_BASE_URI = 760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome
REGISTRY_ID  = 760097843905

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    = snapshot_scheduler snapshot_slack_alarms

TF_NAME =
TF_PATH =

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/functions.Makefile

STACK_ROOT 	= reporting

SBT_APPS 	 =
SBT_SSM_APPS =

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    = reporting_miro_transformer \
                  reporting_miro_inventory_transformer \
                  reporting_sierra_transformer \
                  reporting_calm_transformer

TF_NAME = reporting
TF_PATH = $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = true

$(val $(call stack_setup))

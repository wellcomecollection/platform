ROOT = $(shell git rev-parse --show-toplevel)
STACK_ROOT 	= catalogue_pipeline

include $(ROOT)/functions.Makefile

SBT_APPS 	 =
SBT_SSM_APPS = id_minter ingestor recorder matcher merger

ECR_BASE_URI = 760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome
REGISTRY_ID  = 760097843905

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS     =
PYTHON_SSM_APPS =
LAMBDAS 	    =

TF_NAME = catalogue_pipeline
TF_PATH = $(STACK_ROOT)/terraform

TF_IS_PUBLIC_FACING = false

$(val $(call stack_setup))

include $(ROOT)/$(STACK_ROOT)/transformer/Makefile

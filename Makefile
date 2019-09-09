export ECR_BASE_URI = 760097843905.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome
export REGISTRY_ID  = 760097843905

include makefiles/functions.Makefile
include makefiles/formatting.Makefile

include infrastructure/Makefile
include assets/Makefile
include builds/Makefile
include loris/Makefile
include monitoring/Makefile
include ontologies/Makefile
include nginx/Makefile
include reporting/Makefile

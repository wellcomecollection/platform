export ECR_BASE_URI = 975596993436.dkr.ecr.eu-west-1.amazonaws.com/uk.ac.wellcome
export REGISTRY_ID  = 975596993436

include makefiles/functions.Makefile
include makefiles/formatting.Makefile

include infrastructure/critical/Makefile
include infrastructure/shared/Makefile

include assets/Makefile
include builds/Makefile
include loris/Makefile
include data_science/Makefile
include monitoring/Makefile
include ontologies/Makefile
include nginx/Makefile
include reporting/Makefile
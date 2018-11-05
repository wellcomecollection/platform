# Build and tag a Docker image.
#
# Args:
#   $1 - Name of the image.
#   $2 - Path to the Dockerfile, relative to the root of the repo.
#
define build_image
	$(ROOT)/docker_run.py \
	    --dind -- \
	    $(DOCKER_IMG_IMAGE_BUILDER) --project=$(1) --file=$(2)
endef


# Publish a Docker image to ECR, and put its associated release ID in S3.
#
# Args:
#   $1 - Name of the Docker image.
#
define publish_service
	$(ROOT)/docker_run.py \
	    --aws --dind -- \
	    $(DOCKER_IMG_PUBLISH_SERVICE) \
	        --project="$(1)" \
	        --namespace=uk.ac.wellcome \
	        --infra-bucket="$(WELLCOME_INFRA_BUCKET)" \
			--sns-topic="$(ECR_PUSHES_TOPIC_ARN)"
endef

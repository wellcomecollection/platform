build:
	docker build --tag elasticsearch_cleaner .

deploy: build
	echo $$(git rev-parse HEAD) > $$(git rev-parse --show-toplevel)/.releases/elasticsearch_cleaner
	docker tag elasticsearch_cleaner elasticsearch_cleaner:$$(git rev-parse HEAD)
	$$(git rev-parse --show-toplevel)/scripts/deploy_docker_to_aws.py --project=elasticsearch_cleaner --infra-bucket=platform-infra

build:
	docker build --tag elasticsearch_cleaner .

deploy: build
	$$(git rev-parse --show-toplevel)/scripts/deploy_docker_to_aws.py --project=elasticsearch_cleaner --infra-bucket=platform-infra

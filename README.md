# platform-infra

Infrastructure for Wellcome Digital Platform backend.

We use this infrastructure to deploy [the Wellcome Digital Platform](https://github.com/wellcometrust/platform-api)

## Setup
- [Install Terraform](https://www.terraform.io/downloads.html)
- Ensure your AWS credentials are available as default

## Usage
```sh
cd terraform
./plan.sh

# If your plan looks good then

terraform apply
```

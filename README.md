# platform-infra

This repo defines the infrastructure for the [Wellcome Digital Platform][api].

The structure of the repo reflects parts of our infrastructure:

*   `terraform` - we make heavy use of AWS, and our infrastructure is defined entirely with [Terraform][terraform].
    This directory has all our Terraform config.

*   `lambdas` contains the source code for our [AWS Lambdas][lambda].
    They are configured entirely through Terraform.

*   `docker` contains our custom Docker images.

We also have a [hosted Elastic Cloud][elastic] instance which provides our search index.
Elastic Cloud can't be configured through Terraform, so this is set up by hand.

[api]: https://github.com/wellcometrust/platform-api
[terraform]: https://www.terraform.io
[lambda]: http://docs.aws.amazon.com/lambda/latest/dg/welcome.html
[elastic]: https://www.elastic.co/cloud

## Getting started

1.  Clone the repository:

    ```console
    $ git clone https://github.com/wellcometrust/platform-infra.git
    $ cd platform-infra
    ```

2.  Install Terraform, using [the instructions][terra_install] from the Terraform docs.

3.  Make sure your AWS credentials are [available to Terraform][aws_auth].

[terra_install]: https://www.terraform.io/intro/getting-started/install.html
[aws_auth]: https://www.terraform.io/docs/providers/aws/index.html#authentication

## Usage

First run the plan script:

```console
$ cd terraform
$ ./plan.sh
```

This should print the output of a `terraform plan`, telling you what resources will be changed.
If this looks correct, then you can apply the changes by running:

```console
$ terraform apply terraform.plan
```

## S3 infra bucket

We have an S3 bucket for storing files related to our infrastructure (configured as the backend in `terraform.tf`).
As well as the application config (which is auto-generated from our Terraform), it contains two files that we don't want in a public repo:

*   `platform.tfstate` - our Terraform state file
*   `terraform.tfvars` – key names, our Elastic Cloud config, and so on.
    Any variable listed in [`variables.tf`](terraform/variables.tf) that isn't defined elsewhere.

## License

MIT.

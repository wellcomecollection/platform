# platform-infra

This repo defines the infrastructure for the [Wellcome Digital Platform][api].

We make heavy use of AWS and Docker.
Our AWS infrastructure is defined entirely with [Terraform][terraform] files which are kept in this repository, along with a handful of custom Docker images.

[api]: https://github.com/wellcometrust/platform-api
[terraform]: https://www.terraform.io

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

## License

MIT.

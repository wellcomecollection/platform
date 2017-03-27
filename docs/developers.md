# Developer info

This document contains information for developers working on the platform
codebases.

* [Repos](#repos)
* [Tech stack](#tech_stack)
* [Development workflow](#workflow)

## Repos

There are two main repos for the platform code:

*   [platform-api][api] contains the code for our individual application,
    and is the starting point for the project.
*   [platform-infra][infra] contains the Terraform configuration for our
    AWS infrastructure.

Code that exists as a useful standalone component may be siphoned off into
other, smaller repos as appropriate.

[api]: https://github.com/wellcometrust/platform-api
[infra]: https://github.com/wellcometrust/platform-infra

## Tech stack

Our tech stack uses the following pieces:

*   **Scala, sbt, Java 8** – this is the language in which the majority of
    our aplications are written.
*   **Docker** – our applications are usually deployed as Docker containers
    running in Amazon's EC2 Container Service.
*   The **aws-cli** tool.
*   **Terraform** – for managing out infrastructure.
*   **Python 2.7 with boto3** – Our Lambda functions are written in Scala.

If you're working on the platform team, you should install everything on
this list.  The instructions below assume you're using a relatively modern
version of Mac OS X.

#### Homebrew

[Homebrew][brew] is a package manager for OS X that's used to install
several of our dependencies.

Run the following command in a Terminal prompty:

```console
$ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

[brew]: https://brew.sh/

#### Scala, sbt, Java 8

Install with Homebrew:

```console
$ brew cask install java
$ brew install sbt scala scalafmt
```

#### Docker

Download the [Docker for Mac installer][docker].

(This has a brew formula, but nobody on the platform team has tested it.)

[docker]: https://docs.docker.com/docker-for-mac/install/

#### Terraform

Install with Homebrew:

```console
$ brew install terraform
```

#### Python 2.7

Python 2.7 is already installed on OS X.

#### aws-cli

My preferred approach for installing the AWS command-line tools, which
doesn't involve faff with virtualenvs, is to use [pipsi][pipsi]:

```console
$ curl https://raw.githubusercontent.com/mitsuhiko/pipsi/master/get-pipsi.py | python
$ pipsi install aws-cli
```

You'll also need to put your AWS credentials in `~/.aws/credentials`.

[pipsi]: https://github.com/mitsuhiko/pipsi

## Our development workflow

We use feature branches for development.

*   When you start work on a new change, cut a branch from the tip of master
    and start working on it locally.
*   Once your change is ready for review, open a pull request to merge it back
    into master.  It doesn’t necessarily need to be finished – WIP pull
    requests are encouraged!
*   All pull requests must be reviewed, ideally by somebody who didn’t write
    the code.  This helps ensure we all stay familiar with the codebase, and
    changes are checked for quality.

When developing a new feature, cut a branch from the tip of master and
develop locally.  Open a pull request to merge your feature back into master.

We don't run automated linters over patches – it’s up to committers to do
formatting changes, or reviewers to flag if not.  We use two auto-formatters:

```console
$ scalafmt
$ terraform fmt
```

will bring your code into line.

## How deployment works

To deploy a change to an application:

1.  **Open a pull request.**  This is built with Circle CI to check that it
    compiles and passes tests.  Pull requests are only merged when they have
    a green tick from Circle.

2.  **Merge to master.**  This triggers a full build in Circle CI, which does
    a test, builds the application, and pushes new container images to the EC2
    Container Repository (ECR).

3.  **Run a `terraform apply`.**  Running a terraform apply with the new
    release ID will upgrade all the apps to the new version.  (We don't yet
    have the ability to do a targeted deploy of one application.)

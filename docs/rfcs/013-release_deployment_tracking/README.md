# RFC 013: Release & Deployment tracking

**Last updated: 24 January 2019.**

## Background

We build container images as artifacts for deployment, they are labelled with the git ref at the code point the artifact was packaged.

Deploying container images to production at present moves all container images forward to the latest built version.

## Problem statement

We wish to track multiple environments with container images at different versions, and have an audit trail for deployments to those environments.

A deployment mechanism to take a container image for a service within a project and run that in a given environment is deliberately not described here.

SSM parameters are heavily favoured as they provide a data source for terraform.

## Proposed Solution

This solution describes a set of steps to track build, release and deployment but is deliberately agnostic of deployment mechanism. In addition this RFC **does not** describe a mechanism for reporting build or deployment status.

### Build artifacts

> **Build artifact:** A container image encapsulating application code that can be configured by environment variable. The artifact _does not_ contain configuration.

When container images intended for release are built they will be added to an ECR repository in AWS.

#### Build artefacts SSM Parameters

The URI of the container image will then be used to update an SSM parameter with a key:

 `/{project_id}/artefacts/{label}/{service_id}`

The attributes are described as follows:
- **project_id**: an identifier for a project.
- **label:** A label for the set of images released, e.g. latest, stable, v2-branch

 For example:

 `/storage/images/latest/archivist`

SSM parameters will provide a versioned record of build artifacts. SSM allows descriptions to be added to updates, these descriptions should contain the `user_id` of what or who is updating the version

This mechanism will be provided via a python application, packaged in a docker container distributed via the https://github.com/wellcometrust/dockerfiles repository.

### Project structure

> **Project:** A set of services that when composed perform a function.

In order to build releases that describe which version of a service to deploy to a particular environment we need a machine readable description of project structure.

We propose the following structure:

```json
{
  "project": {
    "id": "id",
    "name": "project_name"
  },
  "services" : [
    {
      "id": "service_id",
      "name": "service_name"
    }
  ]
}
```

This is a **project manifest**.

Where this is a file `.wellcome_project` in the project root.

This file can be created by hand or by script.

### Releases & deployments

> **Release:** A set of services at known versions to be deployed to an environment.

> **Deployment:** A statement of intent to deploy a given _release_ to a particular environment.

We propose the following structure to track releases & deployments for a particular project:

#### Release manifest

```json
{
  "project": {
    "id": "project_id",
    "name":  "project_full_name"
  },
  "date_requested": "ISO8601 date",
  "requested_by": {
    "id": "user_id",
    "name":  "user_full_name"
  },
  "description": "some text describing the content of the release",
  "images": {
    "service_id": "container_image_uri"
  },
  "deployments": [
    {
      "environment": {
        "id": "environment_id",
        "name":  "environment_full_name"
      },
      "date_requested": "ISO8601 date",
      "requested_by": {
        "id": "user_id",
        "name":  "user_full_name"
      },
      "description": "some text describing the reason for deploying the release to the given environment"
    }
  ]
}
```

The container image URIs can be sourced from the SSM Parameters:

| key  	                                    | value                                         |
|---	                                    |---	                                        |
| /{project_id}/images/{label}/{service_id} | http://example.com/images/{service_id}/00001  |

#### Releases table

Release manifests should be kept in a dynamo table that contains the following attributes:

| release_id  | project_id        |created_at    |deployed_at    |release_manifest   |
|---          |---                |---           |---            |---                |
| abcd        | project_id        | 1548345406   | 1548345406    | {}                |

The attributes are described as follows:
- **release_id**: (_hash key_) an identifier for a project.
- **project_id**: an identifier for a project.
- **created_at:** Unixtime representation of time the record was written.
- **deployed_at:** Unixtime representation of the time a deployment was updated.
- **release_manifest:** JSON blob representing the release manifest.

The following GSIs (hash key/range key)are suggested for making it easy to lookup releases & deployments:

| hash key      | range key     | purpose              |
|---            |---            |---                   |
| project_id    | created_at    | latest releases      |
| project_id    | deployed_at   | latest deployments   |

#### Deployments SSM Parameters

A deployment would take the URIs for a service provided by a release manifest and update the following keys in SSM:

| key  	                                                    | value     |
|---	                                                    |---	    |
| /{project_id}/deployments/{environment_id}/{service_id}   | {ecr_uri} |

The attributes are described as follows:
- **project_id**: an identifier for a project.
- **environment_id:** an id identifying a deployment environment, e.g. dev,stage,prod
- **ecr_uri**: the ECR URI of the container image to deploy.

#### Deployments table

In order to track current deployments a table with the following structure is required:

| project_id | environment_id  | release_id |
|---         |---              |---         |
| my_project | stage           | abcd       |

The attributes are described as follows:
- **project_id**: (hash key) an identifier for a project.
- **environment_id:** (range key) an id identifying a deployment environment, e.g. dev,stage,prod
- **release_id**: the UUID of a release from the releases table.

**The deployments table will be updated atomically with the releases table when it is updated to add a deployment.**

This mechanism will be provided via a python application, packaged in a docker container distributed via the https://github.com/wellcometrust/dockerfiles repository.

### Examples

#### Deploying the latest images to a stage environment.

In order to create a release we can list the ECR URIs available in SSM for a particular project and label by looking at the services described in the project manifest.

For example for a project with the manifest:

```json
{
  "project": {
    "id": "bugfarm_73649",
    "name": "The Best Bug Farm"
  },
  "services" : [
    {
      "id": "hatching",
      "name": "Big Bob's baby bug boomer!"
    },
    {
      "id": "feeding",
      "name": "Fragile Freddie's frosty feeders!"
    }
  ]
}
```

With the images label `latest` we would look in SSM for the following parameters:

| key  	                                    | value                                     |
|---	                                    |---	                                    |
| /bugfarm_73649/images/latest/hatching  	| http://example.com/images/hatching/00001  |
| /bugfarm_73649/images/latest/feeding 	    | http://example.com/images/feeding/00001  	|

This would result in a release manifest as follows:

```json
{
  "project": {
    "id": "bugfarm_73649",
    "name":  "The Best Bug Farm"
  },
  "date_requested": "2008-09-15T15:53:00",
  "requested_by": {
    "id": "dev_dave",
    "name":  "Dave Dollop"
  },
  "description": "Some stuff wot I did.",
  "images": {
    "hatching": "http://example.com/images/hatching/00001",
    "feeding": "http://example.com/images/feeding/00001"
  },
  "deployments": []
}
```

The releases table being updated to:

| release_id  | project_id        |created_at    |deployed_at    |release_manifest   |
|---          |---                |---           |---            |---                |
| abcd        | bugfarm_73649     | 1548345406   |               | _as above_        |

Requesting a deployment would result in the release manifest being updated as follows:

```json
{
  "project": {
    "id": "bugfarm_73649",
    "name":  "The Best Bug Farm"
  },
  "date_requested": "2008-09-15T15:53:00",
  "requested_by": {
    "id": "dev_dave",
    "name":  "Dave Dollop"
  },
  "description": "Some stuff wot I did.",
  "images": {
    "hatching": "http://example.com/images/hatching/00001",
    "feeding": "http://example.com/images/feeding/00001"
  },
  "deployments": [
      {
        "environment": {
          "id": "stage",
          "name":  "Simon's staging sideshow!"
        },
        "date_requested": "2008-09-16T14:33:00",
        "requested_by": {
          "id": "ops_ophelia",
          "name":  "Ophelia Oppenheimer"
        },
        "description": "Some stuff wot dave did for testing."
      }
  ]
}
```

The deployments table being updated to:

| project_id    | environment_id  | release_id |
|---            |---              |---         |
| bugfarm_73649 | stage           | abcd       |

The releases table being updated to:

| release_id  | project_id        |created_at    |deployed_at    |release_manifest   |
|---          |---                |---           |---            |---                |
| abcd        | bugfarm_73649     | 1548345406   | 1638345401    | _as above_        |

SSM would be updated to:

| key  	                                    | value                                     |
|---	                                    |---	                                    |
| /bugfarm_73649/deployments/stage/hatching | http://example.com/images/hatching/00001  |
| /bugfarm_73649/deployments/stage/feeding 	| http://example.com/images/feeding/00001  	|

And our imaginary deployment mechanism would attempt to make the environment match this state.

#### Promoting a staging release to production

In order to push the state of environment stage -> prod (or any env a -> b), following from the previous example we query our deployments table:

```py
table.query(
  KeyConditionExpression=Key('project_id').eq('bugfarm_73649') & Key('environment_id').eq('stage')
)
```

The response should include our `release_id`, then we query the releases table:

```py
table.query(
  KeyConditionExpression=Key('release_id').eq('abcd')
)
```

The response includes our release manifest from which we can identify the images required by a particular release:

```json
{
  "project": {
    "id": "bugfarm_73649",
    "name":  "The Best Bug Farm"
  },
  "...",
  "images": {
    "hatching": "http://example.com/images/hatching/00001",
    "feeding": "http://example.com/images/feeding/00001"
  },
  "..."
}
```

We can now request a fresh deployment as above using those images, the final state being:

**Release manifest:**

```json
{
  "project": {
    "id": "bugfarm_73649",
    "name":  "The Best Bug Farm"
  },
  "date_requested": "2008-09-15T15:53:00",
  "requested_by": {
    "id": "dev_dave",
    "name":  "Dave Dollop"
  },
  "description": "Some stuff wot I did.",
  "images": {
    "hatching": "http://example.com/images/hatching/00001",
    "feeding": "http://example.com/images/feeding/00001"
  },
  "deployments": [
    {
      "environment": {
        "id": "stage",
        "name":  "Simon's staging sideshow!"
      },
      "date_requested": "2008-09-16T14:33:00",
      "requested_by": {
        "id": "ops_ophelia",
        "name":  "Ophelia Oppenheimer"
      },
      "description": "Some stuff wot dave did for testing."
    },
    {
      "environment": {
        "id": "prod",
        "name":  "Peters prod palace!"
      },
      "date_requested": "2008-09-17T14:33:00",
      "requested_by": {
        "id": "ops_ophelia",
        "name":  "Ophelia Oppenheimer"
      },
      "description": "Some stuff wot dave did for releasing."
    }
  ]
}
```

The deployments table being updated to:

| project_id    | environment_id  | release_id |
|---            |---              |---         |
| bugfarm_73649 | stage           | abcd       |
| bugfarm_73649 | prod            | abcd       |

The releases table being updated to:

| release_id  | project_id        |created_at    |deployed_at    |release_manifest   |
|---          |---                |---           |---            |---                |
| abcd        | bugfarm_73649     | 1548345406   | 1738545402    | _as above_        |

SSM would be updated to:

| key  	                                    | value                                    |
|---	                                    |---	                                   |
| /bugfarm_73649/deployments/prod/hatching | http://example.com/images/hatching/00001  |
| /bugfarm_73649/deployments/prod/feeding 	| http://example.com/images/feeding/00001  |

And again our imaginary deployment mechanism would attempt to make the environment match this state.

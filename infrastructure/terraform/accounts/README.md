## Developer Access to AWS

All Wellcome Collection accounts are provisioned using Terraform. The modules in `terraform/accounts/modules` create a set of roles for use as a developer (and for other specialised purposes).

The platform account allows federated identity from Azure AD from which all other user permissions should flow.

## Identity

New users can be added by other authorized users using the Azure Active Directory AWS SSO app.

You can sign-in via http://wellcomecloud.org.

### Assiging role-sets

Assigning users to role sets is done in the Azure AWS Application.

You can sign-in to view and edit role set assignation if authorised to do so.

### Switching Roles

Providing an account has enabled access to a principal which includes a users identity they can switch to that role.

In the console this can be done by choosing to switch role and providing the account identity and role to switch to.

You can also use these links:

- Experience account: [experience-dev](https://signin.aws.amazon.com/switchrole?roleName=experience-dev&account=130871440101)

- Platform account: [platform-dev](https://signin.aws.amazon.com/switchrole?roleName=platform-dev&account=760097843905)

- Storage account: [storage-dev](https://signin.aws.amazon.com/switchrole?roleName=storage-dev&account=975596993436)

- Workflow account: [workflow-dev](https://signin.aws.amazon.com/switchrole?roleName=workflow-dev&account=299497370133)

- wellcomecollection.org Hosted zones access: [wc-route53](https://signin.aws.amazon.com/switchrole?roleName=wellcomecollection-assume_role_hosted_zone_update&account=250790015188)

We recommend you use the `aws-extend-switch-roles` Chrome/Firefox extension: https://github.com/tilfin/aws-extend-switch-roles. A pre-baked config is [available](extension_config).

### Getting CLI credentials

In order to obtain CLI credentials you will need to intercept details from the SAML sign-in process.

We recommend using the following node app https://github.com/sportradar/aws-azure-login.

In configuration options you can choose to set up the roles to generate credentials for e.g.

| Profile name      | Role ARN
|---                |---                                                    |
| workflow-dev      | arn:aws:iam::299497370133:role/workflow-developer     |
| storage-dev       | arn:aws:iam::975596993436:role/storage-developer      |
| platform-dev      | arn:aws:iam::760097843905:role/platform-developer     |
| experience-dev    | arn:aws:iam::130871440101:role/experience-developer   |

You can symlink the generated credentials file to your AWS credentials file in order to automatically refresh your credentials at sign-in.

### Decrypting terraform outputs

Provided you have the correct ssh keys and Keybase you can decrypt terraform outputs as follows.

```
terraform output tf_output_name | base64 -D | keybase pgp decrypt
```

Platfrom SSH Keys available to authorised users in the `wc_platform` Keybase team.

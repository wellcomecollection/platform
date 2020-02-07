# Platform Infrastructure

Various terraform stacks for handling Wellcome Collection digital platform infrastructure.

See:
- [terraform/critical](critical/README.md): Any infrastructure where extreme care must be taken to prevent deletion of data.
- [terraform/accounts](accounts/README.md): Provisioning AWS account access.
- [terraform/dns](dns/README.md): Managing the infrastructure for Wellcome Collection's DNS.
- terraform/shared: Everything else that hac cross platform concerns e.g. logs, configuration, networking.
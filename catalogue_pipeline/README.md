# catalogue_pipeline

The stack containing the Catalogue Pipeline services

## Overview

Contains:
- ingestor: Ingests documents into Elasticsearch
- id_minter: Assigns unique identifiers to documents
- transformer: Transforms from source to unified data model
- adapters: Keeps local dynamodb data stores up to date with content from external systems
    - miro_adapter
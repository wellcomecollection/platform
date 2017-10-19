# sierra_adapter

This stack is responsible for reading information from the Sierra API and copying it into DynamoDB.

Once it's in DynamoDB, it's picked up by the standard Catalogue pipeline (transformer, ID minter, etc.) and processed for Elasticsearch ingest.

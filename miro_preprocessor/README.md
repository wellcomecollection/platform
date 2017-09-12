# miro_preprocessor

This stack is responsible for sorting Miro metadata and assets into one of three piles:

*   Cold store / Amazon Glacier
*   Tandem Vault
*   The Catalogue API

## xml_to_json_converter

An ECS Task with inputs:

- S3 Source location (a Miro formatted XML dump)
- S3 Target location

This task downloads and converts a Miro formatted XML dump to JSON and puts it at the S3 target location specified.
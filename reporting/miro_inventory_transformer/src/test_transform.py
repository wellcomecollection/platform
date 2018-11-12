import json
from transform import transform


def raw_data():
    return json.loads(
        """
        {
        "catalogue_api_derivative": true,
        "catalogue_api_derivative.bucket": "some-bucket",
        "catalogue_api_derivative.key": "some-key",
        "catalogue_api_master": true,
        "catalogue_api_master.bucket": "some-bucket",
        "catalogue_api_master.key": "some-key",
        "catalogue_entry.id": "some-id",
        "cold_store.bucket": null,
        "cold_store.key": null,
        "cold_store_master": false,
        "extra_s3_objects": [],
        "id": "some-id",
        "tandem_vault.asset_id": null,
        "tandem_vault.bucket": null,
        "tandem_vault.key": null,
        "tandem_vault_master": false
        }
        """
    )


def test_json_parses():
    transformed_data = transform(raw_data())
    assert isinstance(transformed_data, dict)

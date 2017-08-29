# update_api_docs

This task publishes a new Swagger definition to our documentation, which is hosted on [Spotlight][spotlight].

It performs an [Import][import] and a [Publish][publish], which requires the documentation to have been published at least once before.

[spotlight]: https://stoplight.io/
[import]: https://help.stoplight.io/api-v1/versions/import
[publish]: https://help.stoplight.io/api-v1/versions/publish

## Usage

The task takes three parameters:

*   `VERSION_ID` -- the [version identifier][version] for the docs
*   `API_SECRET` -- your API secret, as provided by [the Stoplight API][auth].
*   `SWAGGER_URL` -- the URL of the Swagger file to use.
    This defaults to <https://api.wellcomecollection.org/catalogue/v0/swagger.json>.

[version]: https://help.stoplight.io/api-v1/versions/working-with-versions
[auth]: https://help.stoplight.io/api-v1/api-introduction/authentication

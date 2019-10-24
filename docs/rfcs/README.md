# RFCs

RFCs (or [Request for Comments](https://en.wikipedia.org/wiki/Request_for_Comments)) are how we record design decisions in the platform.

We have the following RFCs:

<dl>
  <dt><a href="001-merger_matcher">001: Matcher architecture</a></dt>
  <dd>
    In the catalogue API, we want to combine works that represent the same "thing".
    (For example, the library record for a manuscript and a digitised version of the same.)
    This RFC explains the architecture and data model of our merger/matcher applications.
  </dd>

  <dt><a href="002-archival_storage">002: Archival Storage Service</a></dt>
  <dd>
    We have a storage service that we use as the content store for our digital assets (digitised archives, born-digital holdings, and so on).
    This RFC describes the external-facing API, used by tools that want to store or retrieve content in the storage service.
    <p>
      It does not describe the internal architecture of the storage service.
    </p>
  </dd>

  <dt><a href="003-asset_access">003: Access to sensitive assets</a></dt>
  <dd>
    Some of the Collection's digital assets contain sensitive content, and need authentication before they can be accessed.
    (For example, archival material that is less than 100 years old.)
    This RFC describes a possible architecture for an authentication system that works with Loris, our IIIF Image API server.
  </dd>

  <dt><a href="004-mets_adapter">004: METS adapter</a></dt>
  <dd>
    We want to surface data about our digitised works in the Catalogue API.
    The metadata about digitised works is created by Goobi, our workflow software, which produces <a href="https://en.wikipedia.org/wiki/Metadata_Encoding_and_Transmission_Standard">METS files</a>.
    This RFC describes a possible archiecture for a system to feed METS from Goobi into the Catalogue pipeline.
  </dd>
</dl>

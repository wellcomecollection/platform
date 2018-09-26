# catalogue_pipeline

The stack contains the permanent data stores used by the catalogue pipeline.

This includes:

*   The "source data" tables -- VHS instances containing pipeline data copied
    from the source systems (e.g. Sierra, Miro, Calm).

    Although these are theoretically transient (we could reharvest all the
    data from the source systems), it's annoying to do so.  We keep them
    separate to avoid breaking them by accident.

*   The ID minter database -- the mapping from source system IDs to platform
    canonical IDs.

    We cannot regenerate this easily (IDs are assigned randomly), so we want
    to protect this from accidental changes.

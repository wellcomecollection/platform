
# JSON examples: a lower-level object from within an archive hierarchy described in CALM

Comment, lines 8-15
CALM contains both RefNo (to build the hierarchy) and AltRefNo (to show to the user).
identifierScheme indicates which of these identifiers is which and thus what should be done with them.
Also occurs with Item, below

Comment, lines 18-23
"isPartOf" can be inferred from the CALM RefNo: if the RefNo has a slash or slashes in it, the Work should be considered to be part of whatever Work has the same RefNo minus the final slash and whatever follows it
Thus if RefNo = PPCRI/D/1/1/8 we know there must be a PPCRI/D/1/1 that sits above it in the hierarchy
This of course just tells you what the next level up is.  The top level description for the whole collection would be at RefNo = whatever precedes the first slash
Which would we want to expose from a UX POV?

Comment, lines 39-50
CALM contains both RefNo (to build the hierarchy) and AltRefNo (to show to the user).
identifierScheme indicates which of these identifiers is which and thus what should be done with them.
Also occurs with work, above

Comment, line 55
This is the "Reproduction Conditions" field in CALM
In the original ontology .ttl file I called it ipRightsStatement but we weren't sure about that as a property name so have gone for reproduction pro tem

Comment, lines 56-79
I've modelled this statement of how to get access to the physical and digital copies on Silver's MIRO v.2 file (https://github.com/wellcometrust/platform-api/blob/master/ontologies/MIRO_sample_image_of_artwork_v2.json)

Comment, line 61
This concatenates the contents of AltRefNo and Box Number (and/or Outsize Item Number) as is done in the Sierra harvest to create what Sierra presents as Shelfmark

Comment, line 73
This AccessStatus is not in the CALM data but in the METS governing access to the digitised facsimile - we might therefore just have to show this as Open


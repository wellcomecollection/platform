
#JSON examples journals

Comment line 13-22: 
a) are we happy dealing with all these different sorts of title as subsets of alternativeTitlt?
b) if so, keyTitle and uniformTitle need to be added to the ontology and described

Comment line 43: "designation" needs to be added the ontology
It corresponds in part to the start date and end date properties that are already there, covering dates of publication and sequential designation, but has other things mixed in like volumes

Comment line 44 onwards: ISSN is not in the ontology yet
It occupies the same field as ISBN and the distinguishing feature is apparently the format: ISSN has four digits, a slash then another four digits.
Transformer would need to be programmed to understand this distinction

Comment line 57: Format in this instance just duplicates the content of WorkType - surely we don't need both

Comment line 58: frequency can be either current or past: the distinction lies in which MARC field it comes from (310 or 321 respectively)

Comment line 88: numbering is not yet in ontology

Comment line 89: supplement is not yet in ontology

Comment lines 90-108: relatedMaterialNote brings together material fromn MARC 530 - the alternative formats field - and MARC 775, related to.  We would want to distinguish these?

Comment, lines 111-124: MARC field 700 is a field for names addedd to a catalogue record
It is not explicitly for creators according to the standard, but labelled "Author etc." in Sierra, so the mapping here is probably justified

Comment line 144
"volume" is not yet in the hierarchy
it is a journal-specific concept: it might actually map to Title = Label

NB there is also a general note in the .i record, without a MARC number, setting out the imprint details of the early volumes: how to get at that without also bringing notes not intended for the public?




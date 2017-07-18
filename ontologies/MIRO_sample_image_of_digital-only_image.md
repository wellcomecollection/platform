
# JSON samples: MIRO images that are free-standing, without an analogue in other Library systems

Comment on lines 12-13: 
This is a non-standard reference, provided by the organisation that provided the image.  
I assume here that we would be able to set up something where if the label began "Contributor Reference" and the data is from MIRO, we could supply
"MIRO contributor" as an authority

Comment, line 13
The ipRightsStatement here picks up on the MIRO credit line and reproduces it unchanged.  
We might alternatively choose to precede it with some standard text along the lines of "image courtesy of..." 

Comment, lines 20-36
I've not yet set up the Subjects ontology but am assuming that each subject will have a note of its label and its source authority.  NB In MIRO there are keywords that don't come from any external thesaurus


Comment, lines 44-53
The Access status of "Open" (ie can be shown on the web) is generated within MIRO by the combination of five factors: 

* `<all_web_publish>`, `<image_general_use>` and `<image_copyright_cleared>` have Y, 
* `<image_title>` has content, and 
* "cataloguing is complete" - will follow up with MIRO users whether there is a flag for this

Originally this property was attached to the Work but on reconsideration this should probably be attached to the actual Item, as access conditions may vary between items (eg the digital version may be open but the original 35mm slide be inaccessible)

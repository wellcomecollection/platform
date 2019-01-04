# Notes on each of the test b numbers

## Good stuff

I'll only mention it here if something needs attention.

In general, each entry in [b_numbers.md](b_numbers.md) should look identical on uat and live; search should work (where applicable), it should load quickly, etc.

I realised I had missed a part of the DDS - the code that generates jpeg thumbnails for PDFs. This needed some refactoring but it's now quite a nice example of a client of the abstracted storage. I added `WriteFile` to my IWorkStore interface; away from the messy implementation details the mechanics of old or new storage models are nicely hidden, even on a rare occasion like this where I need 

```C#
var manifestation = (IManifestation) metsRepository.Get(legacyId);
var pdfPhysicalFile = manifestation.Sequence.Single(pf => pf.StorageIdentifier == fileIdentifier);
var workStore = manifestation.GetWorkStore();
// Copy the archived bag relative file to a local file path:
workStore.WriteFile(pdfPhysicalFile.RelativePath, tempFile.FullName);
GhostScriptUtil.CreateThumbForPdf(tempFile, cachedThumbnail);
```

The end result: 

https://library-uat.wellcomelibrary.org/pdfthumbs/b28462270/0/DigitalHumanitiesPedagogy.pdf.jpg

Video support looks good, for example:

https://library-uat.wellcomelibrary.org/item/b16641097

This has been bagged, ingested, then processed by the DDS; the DDS has told the DLCS about it, the DLCS has fetched the stored video from the access S3 location and created the derivatives for web presentation; the poster image is still coming from the DDS. So lots of things working together here.

## Not so good stuff

I haven't tried to re run these, so we can look at the problems.

*b16675630* - Video, with transcript (2-part multiple manifestation)  
[migration](http://wt-havana:88/Dash/Migration/b16675630) | [dash](http://wt-havana:88/Dash/Manifestation/b16675630) | [uat](https://library-uat.wellcomelibrary.org/item/b16675630) | [live](https://wellcomelibrary.org/item/b16675630)

See http://wt-havana:88/Dash/Migration/b16675630 for the timings.

This was accepted by the storage service, but then no further events have been recorded.

*b17307922* - Audio, MP3  
[migration](http://wt-havana:88/Dash/Migration/b17307922) | [dash](http://wt-havana:88/Dash/Manifestation/b17307922) | [uat](https://library-uat.wellcomelibrary.org/item/b17307922) | [live](https://wellcomelibrary.org/item/b17307922)

I think this is a problem for the DLCS rather than anything else. The DLCS thinks the audio is still ingesting; possibly it is stuck in transcoding, one for @fractos

*b29524404* - Audio Multiple Manifestation (interview with PDF transcript)  
[migration](http://wt-havana:88/Dash/Migration/b29524404) | [dash](http://wt-havana:88/Dash/Manifestation/b29524404) | [uat](https://library-uat.wellcomelibrary.org/item/b29524404) | [live](https://wellcomelibrary.org/item/b29524404)

This one actually failed:

http://wt-havana:88/Dash/Migration/b29524404

`Invalid bag manifest manifest-sha256.txt`

... so we need to have a close look at what's happening here!







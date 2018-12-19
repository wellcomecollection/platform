# RFC 009: Data model

**Last updated: 19 December 2018.**

## Background

As part of the platform development we create ontologies that describe our collections, events and editorial content as a unified graph of linked data. By using domain modelling and thinking about data as a semantic graph of typed entities and relationships, it helps us to create more richly linked digital experiences that aid exploration and discovery.

The ontologies are documented using OWL. 

We think that this is the best way to formally describe a complex domain model, as it makes the semantics of our data self-documenting and widely shareable.

However, it is worth noting that although we use OWL to document the ontologies, we don’t actually store or process any data as. Our APIs do provide a context, which can be used to transform the data to an RDF model if required, but we consider them JSON-first.

These pages gives an overview of process and the documents describing the models. 

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Uber-wellcome-ontology.png)

## Process

We have used [domain modeling](https://en.wikipedia.org/wiki/Domain_model) as an approach to identify a common model for describing works from different locations in the Wellcome Collection. The emphasise here is on the shared understanding and language between disciplines in the teams.

This enables us to:
* Establish a shared language for things in the model.
* Identify the scope of the model to describe the domain.
* Design a consistent representation of the core bibliographic properties. For example bridging MARC and ISAD(g).

A csv file documents the list of properties identified in the domain modeling sessions. It also provides a summary of how this maps on MARC fields and whether it has been implmented yet.
* (https://github.com/wellcometrust/platform/raw/master/ontologies/WIP/list-of-transformation-tasks.csv)

Reference data:
* Reference data for populating types in the data.
* (https://github.com/wellcometrust/platform/tree/master/ontologies/Reference%20data)

Transformation:
* The transformation file documents in detail how the transformation rule work.
* (https://github.com/wellcometrust/platform/raw/master/ontologies/WIP/sierratransformable)

## Models currently in use

**Core model**

A model describing common and non-domain specific classses and properties used across our ontologies.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Core-Ontology.png)

[ttl file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/core-ontology.ttl)

**Work model**

A model describing library and archive works, including their physical items and relationships.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Work-Ontology.png)

[ttl file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/work-ontology.ttl)

**Location model**

A model describing how museum and library items can be accessed.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Location-Ontology.png)

[ttl file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/location-ontology.ttl)

**Concept model**

A model describing concepts, their classification and relationships.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Concept-Ontology.png)

[ttl file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/concept-ontology.ttl)


## Other models representing Wellcome domains largely work in progress

** Article/Story model**

A model describing editorial articles (stories) and their relationships.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Article-Ontology.png)

[ttl file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/article-ontology.ttl)

**Agency model**

A model describing people, organisations and their relationships.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Agency-Ontology.png)

[ttl file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/agency-ontology.ttl)

**Public event model**

A model describing museum and library events, exhibitions and installations.

![](https://github.com/wellcometrust/platform/raw/master/ontologies/Documentation/Public-Event-Ontology.png)

[tt file](https://github.com/wellcometrust/platform/raw/master/ontologies/Schema/public-event-ontology.ttl)

## Authorities
A mapping has been conducted from a candidate model for people and their positions.
* (https://github.com/wellcometrust/platform/raw/master/ontologies/WIP/Person%20authority-Table.csv)

Example
* (https://github.com/wellcometrust/platform/raw/master/ontologies/Examples/michael-ashburner-example.json)

## Archives
Example of applying work model to a CALM archive record:
* (https://github.com/wellcometrust/platform/raw/master/ontologies/Examples/ashburner-record-example.json)



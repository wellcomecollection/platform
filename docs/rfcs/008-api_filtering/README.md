# RFC 007: API Filtering

**Last updated: 19 October 2018.**

## Problem statement

To ensure a consistent developer experience across our APIs, we need to agree a set of patterns for filtering and sorting. This document outlines our approach.

## Suggested solution

There are three complementary parts to this:

- structured filtering
- full text querying
- sorting

### Structured filtering

Structured filters are non-scoring (ie, they do not result in relevance scores) queries that can be used to restrict the resources returned, without influencing the order in which they are returned.

Structured filters have the following characteristics:

- They are supplied as query parameters
- They have the name of the property they are used filter
- Multiple query parameters are combined using `AND`
- Multiple comma (`,`) separated values within a single query parameter are combined using `OR`
- Closed-open ranges are supported using `/`
- Dotted syntax is used to specify paths
- `id` is implicit in parameter paths

Examples:

Filter for a single work type:

```
/works?workType=book
```

Filter for multiple work types:

```
/works?workType=book,ebook
```

Filter for multiple work types with a single subject:

```
/works?workType=book,ebook&subjects.concepts={id}
```

### Full text querying

Full text querying is supported as a scoring query that can be used to restrict the resources returned, by default returning them in relevance order.

Full text querying has the following characteristics:

- It is supplied as a single query parameter
- The query parameter is called `query`
- It can be combined with any combination of structured filters
- Some field based and other advanced full text querying may be supported, but is service dependent

Examples:

Find all works that mention skeletons:

```
/works?query=skeletons
```

Find all works that mention skeletons and are books or ebooks:

```
/works?query=skeletons&workType=book,ebook
```

### Sorting

The default ordering for a collection of resources is service dependent. It can also vary for different kinds of resource within the same service, to ensure that the default ordering is the most appropriate in the majority of cases.

In addition to this:

- Use of full text querying should change the default order to ordering by relevance
- This can be overridden using a `sort` parameter
- The value of this parameter is a comma separated list of properties
- The order within a property can be changed using `asc` and `desc` modifiers

Example:

```
/works?query=skeletons&sort=year desc,title
```

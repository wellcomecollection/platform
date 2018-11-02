# RFC 001: Matcher architecture

**Last updated: 02 November 2018.**

## Background

Items can be closely related, for example a book may be both a printed book and an eBook, or a painting can include a physical painting and photographs or xray
imagery of the painting.
Where works are so closely related that they represent or refer to the same 'thing' we wish to 
merge them into combined works to aid understanding and searching.

## Problem Statement

Individual works can be merged to form larger combined works. Combined works can be later split.
Each individual work has a unique source identifier. The source identifier for the merged works will be one of the source
identifier of the original works that compose it.
The order in which updates to different nodes appear is irrelevant, but we need to enforce that updates to the same node are applied in order.

The merging of works is broken into two phases

 * a linking phase that identifies linked works to be merged.
 * a merging phase that merges works to form new combined works.
   
The matcher receives a transformed work and determines how it links to other works.

The merger receives the identifiers from the matcher and reads the transformed works from the recorder table.
It then merges the transformed works into new combined works. This means that the merger needs a strategy for how to combine existing works into one.
Among other things it will choose which of the original source identifiers to use for the merged work.
The source identifiers that are not choosen for the merged work will redirect to the merged work.
The merger will put redirect information into those works.

## Proposed Solution

This document has notes on the proposed architecture.

## Model

A matching phase is introduced to determine where works need to be combined.

The input to this phase is a work. A transformed work contains the work identifier, the work version and a list of identifiers of other works that the current work directly links to.
The output is sets of identifiers of affected works alongside their versions. The versions of directly affected works will come from the matcher database.
The merger will use the identifiers to read the works from the recorder VHS and it will use the versions in
the message to ensure that the versions are still the most up to date ones. If so, it will combine the works. If not, it will discard the message.

Example:
```[json]
    {
       "linked-works-sets" : [
            {
                "linked-works": [
                    {
                      "identifier": "sierra-system-number/b1234567",
                      "version": 2
                    },
                    {
                      "identifier": "miro-image-number/V003456",
                      "version": 1
                    },

                ]
            },
            {
                "linked-works": [
                  {
                    "identifier": "sierra-system-number/b876543",
                    "version": 2
                  }
                ]
            }
        ]
    }
```

## Storage

When the matcher receives an update for a work, it needs to know about previously seen works that referenced it to be able to arrange them into sets of linked works.
This implies storing each work that is sees, along to the set of linked works it belongs to at that point in time.
Each set of linked works should have an indentifier that is deterministic on the nodes that compose it. The identifiers of set of linked works should never be exposed outside the matcher.

Similarly, the matcher needs to be able to break connections if a link from one work to another is removed.
This mean storing, for each work, the list of works directly referenced.

The matcher needs to enforce that updates to the same node are applied in order. This means that it needs to store the version for each and that it needs to check that the
version for each update is greater than the currently stored one.
In order to output the version for each node in the output, it needs to have previously seen all nodes affected by an update and stred thei versions.
If it doesn't know the version of some of the nodes because it hasn't seen them yet, it should set their version in the output as 0.

## Database schema

The id used for combined works is created by concatenating the sorted, namespaced source idenfifiers of their components.
For example if a work A is edited to include work B this will be identified in the 'identifiers' field of A.

For example creating a combined work 'A-B' when processing work 'A' could give as input:
```[json]
{
  "identifiers" : [
    // A
    { 
      "identifierScheme": "sierra-system-number",
      "ontologyType": "Work",
      "value": "A"
    },
    // B
    {
      "identifierScheme": "miro-image-number",
      "ontologyType": "Work",
      "value": "B"
    }
  ],
  "version": 2,
  "sourceIdentifier":
    {
      "identifierScheme": "sierra-system-number",
      "ontologyType": "Work",
      "value": "A"
    }
}
```
Note that `A.identifiers` includes the source identifier for  `A` itself.
In the case that neither A or B has previously been identified as a part of a combined work the matcher
will output (assuming that B is stored as version 1 in the matcher database):
```[json]
    {
       "linked-works-sets" : [
            {
                "linked-works": [
                    {
                      "identifier": "sierra-system-number/A",
                      "version": 2
                    },
                    {
                      "identifier": "miro-image-number/B",
                      "version": 1
                    }
                ]
            }
        ]
    }
```

For example, suppose works `A`, `B`, and `A-B` have been previously created and `A` is 
edited to link to `C`, then the output from the matcher would be:
```[json]
    {
       "linked-works-sets" : [
            {
                "linked-works": [
                    {
                      "identifier": "sierra-system-number/A",
                      "version": 2
                    },
                    {
                      "identifier": "miro-image-number/B",
                      "version": 1
                    },,
                    {
                      "identifier": "miro-image-number/C",
                      "version": 3
                    }
                ]
            }
        ]
    }
```


## Concurrent updates and locking

While the required updates for a merged work are being found there cannot be changes to the records for participating 
works.
A locking mechanism is used to achieve this, with all participating works being locked for editing.

## Storage of combined works and locks

Use dynamoDB with a works and a lock table. When a process tries to get a lock on a record in dynamo, it will first do a conditional
put on the locktable with the id of the node that it's trying to lock on. The condition should be that the node id doesn't already exists in the locktable.

If a process is unable to acquire a lock, it should release any previously acquired locks and fail. Releasing the lock means deleting all rows that it has written in the locktable.
This implies that it should be able to identify all rows in the locktable that belong to the current process.

TODO: How to identify rows for the current process? (An id based on the updated work id and version?)

TODO: Strategy for cleaning the table if the application crashes (DynamoDB expire od records?)

## Examples

The matcher is best understood with examples.

## Example 1

Suppose we have the following graph:

![](matcher_example1.png)

We have two existing works: ABC and DEF.
We receive an update to B telling us it now has edges B→A and B→D.

0.  The existing DB is as follows:

        work_id   | links | linked_works_set | version
        A         | B     | ABC              | 2
        B         | A     | ABC              | 1
        C         | B     | ABC              | 2
        D         | F     | DEF              | 3
        E         | D     | DEF              | 2
        F         | -     | DEF              | 4


1.  Because we have an update that affects A, B and D, we read those rows from
    the database first:

        A     | B      | ABC | 2
        B     | A      | ABC | 1
        D     | F      | DEF | 3

2.  By looking at their linked_works_set id, we can do a second read to gather
    all the vertices that might be affected: ABCDEF.
    This gives us the database above, and enough to construct the entire graph
    we're interested in.

3.  Apply the changes, and work out what the new components are.  Write that
    back to the database.

        work_id   | links | linked_works_set | version
        A         | B     | ABCDEF           | 2
        B         | AD    | ABCDEF           | 2
        C         | B     | ABCDEF           | 2
        D         | F     | ABCDEF           | 3
        E         | D     | ABCDEF           | 2
        F         | -     | ABCDEF           | 4

4.  The output JSON is:
```[json]
{
  "linked-works-sets":[
    {
      "linked-works": [
        {
          "identifier": "A", 
          "version": 2
        }, 
        {
          "identifier": "B", 
          "version": 2
        }, 
        {
          "identifier": "C", 
          "version": 2
        }, 
        {
          "identifier": "D", 
          "version": 3
        },
        {
          "identifier": "E", 
          "version": 2
        },
        {
          "identifier": "F", 
          "version": 4
        } 
      ]
    }
  ]
}
```

## Example 2

What if we have two conflicting updates?

The graph of works is fairly sparse, and most of the time edits will either be
no-ops or affect entirely disconnected portions of the graph.

But let's suppose we have the following graph, and receive two updates that
overlap (deliberately not numbered as there's no ordering on updates to
different vertices).

![](matcher_example2.png)

0.  The existing DB is as follows:

            work_id   | links | linked_works_set | version
            A         | B     | ABC              | 2      
            B         | A     | ABC              | 2      
            C         | B     | ABC              | 2      
            D         | F     | DEF              | 3      
            E         | D     | DEF              | 2      
            F         | -     | DEF              | 4      
            G         | -     | G                | 1

1.  Update (*) is processed, and it affects nodes B and E.
    So the worker handling (*) acquires a row-level lock on those two nodes.

    Meanwhile update (**) is also being processed, and it affects F and G.
    So the worker handling (**) F and G acquires a lock on those two rows.

            work_id   | links | linked_works_set | version   
            A         | B     | ABC              | 2         
        *   B         | A     | ABC              | 2         
            C         | B     | ABC              | 2         
        *   D         | F     | DEF              | 3         
        **  E         | D     | DEF              | 2         
        **  F         | -     | DEF              | 4         
            G         | -     | G                | 1         
2.  Process (*) discovers that it affects vertices ABCDEF.

    Process (**) discovers that it affects vertices DEFG.

    Both of them attempt to expand to lock all the vertices they affect -- and
    whoever tries first will hit the other's lock.  They then release their
    lock, and allow the other update to proceed.

    The update will go back into the SQS queue, and will be retried until it
    can be applied conflict-free.

This results in an eventually consistent graph, which is as good as we can
guarantee.

3. Eventually the output JSON is:

```[json]
{
  "linked-works-sets": [
    {
      "linked-works":[  
        {
          "identifier": "A", 
          "version": 2
        }, 
        {
          "identifier": "B", 
          "version": 3
        }, 
        {
          "identifier": "C", 
          "version": 2
        }, 
        {
          "identifier": "D", 
          "version": 3
        }, 
        {
          "identifier": "E", 
          "version": 2
        }, 
        {
          "identifier": "F", 
          "version": 5
        }, 
        {
          "identifier": "G", 
          "version": 1
        }
      ]
    }
  ]
}
```

## Example 3

What happens if we remove a link?

![](matcher_example3.png)

In this example A, B and C are connected into a component called ABC. We receive an update to C that causes ABC to be split.

0.  The existing DB is as follows:

            work_id   | links | linked_works_set | version   
            A         | B     | ABC              | 5         
            B         | A     | ABC              | 3         
            C         | B     | ABC              | 1         

1.  Because we have an update that affects C, we read and acquire a lock on C from the database first:

            work_id   | links | linked_works_set | version  
            C         | B     | ABC              | 1        

2.  By looking at their connected components, we acquire a lock on all other vertices affected: A and B.
3.  We update C to not belong to ABC:

            work_id   | links | linked_works_set | version  
            C         | _     | C                | 2        

4.  We assign A and B to linked_works_set AB:

            work_id   | links | linked_works_set | version  
            A         | B     | AB               | 5        
            B         | A     | AB               | 3

5.  The database ends up looking like this:

            work_id   | links | linked_works_set | version 
            A         | B     | AB               | 5       
            B         | A     | AB               | 3       
            C         | _     | C                | 2       

7.  The output JSON is:

```[json]
{
  "linked-works-sets": [
    {
      "linked-works":[  
        {
          "identifier": "A", 
          "version": 5
        },  
        {
          "identifier": "B", 
          "version": 3
        }
      ]
    },
    {
      "linked-works": [  
        {
          "identifier": "C", 
          "version": 2
        } 
      ]
    }
  ]
}
```
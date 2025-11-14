[![Build with Maven](https://github.com/openmrs/openmrs-module-queue/actions/workflows/maven.yml/badge.svg)](https://github.com/openmrs/openmrs-module-queue/actions/workflows/maven.yml)

OpenMRS Queue Module (backend)
==========================

> OpenMRS backend module for outpatient queue management.
> 
> https://talk.openmrs.org/t/backend-support-for-service-delivery-queues-workflows/35247

## Prerequisites

- OpenMRS Platform ≥ 2.3.x
  - Specifically REST web services
- Java 8 or higher

## Configuration

### Queue Locations

Any Location in the system for which one intends to support Queues should be tagged as a `Queue Location`

### Global Properties

After installing the queue module, configure the following global properties according to your implementation needs.

#### queue.serviceConceptSetName

**Default Value:**  None

**Required?**  True

**Description:**  
A reference to a Concept Set that contains the default allowed values for the `service` property of `Queue`.
As a Queue is essentially a `Service` at a `Location`, this should reflect superset of possible Queue services.

#### queue.statusConceptSetName

**Default Value:**  None

**Required?**  True

**Description:**  
A reference to a Concept Set that contains the default allowed values for the `status` property of `QueueEntry`. 
As a reference, this can be configured as either a Concept uuid, a `source:mapping` mapping, or a unique concept name
This serves as a default for all Queues.  Individual queues can configure a different set of allowed statuses
directly as a property the queue.
A typical set of statuses might include:  `Waiting for Service` and `In Service`

#### queue.priorityConceptSetName

**Default Value:**  None

**Required?**  True

**Description:**  
A reference to a Concept Set that contains the default allowed values for the `priority` property of `QueueEntry`.  
As a reference, this can be configured as either a Concept uuid, a `source:mapping` mapping, or a unique concept name
This serves as a default for all Queues.  Individual queues can configure a different set of allowed priorities
directly as a property the queue.
A typical set of priorities might include:  `Normal` and `Emergency`

#### queue.sortWeightGenerator

**Default Value:**  No global property value, which is equivalent to configuring the `existingValueSortWeightGenerator`

**Required?**  False

**Description:**  
This provides a means to configure the sort weight generator that maintains the primary ordering of Queue Entries on
a particular Queue.  By default, the `existingValueSortWeightGenerator` will be utilized.

### Sort Weight Generators

As described above in Global Property configuration, one can configure the specific algorithm to use to generate and 
store the `sortWeight` property of a QueueEntry.  By default, when QueueEntries are retrieved for a given query, 
they will be returned in order based on the following properties:

1. `sortWeight` descending
2. `startedAt` ascending
3. `dateCreated` ascending

Thus, the first property used in determining the order of entries returned is the `sortWeight`.  One can configure the
system to set this sort weight as follows:

#### existingValueSortWeightGenerator

This is essentially a non-operation.  This will simply use whatever value is explicitly set for `sortWeight`,
or `0` if no value is explicitly set.  This allows consumers that do not explicitly set `sortWeight` to essentially 
ignore this property, and entries will be returned primarily by `startedAt`, leading to a FIFO queue.  It also provides
consumers the flexibility to take full control over the `sortWeight` and set it to whatever value they desire to explicitly
order queue entries as needed.

#### basicPrioritySortWeightGenerator

This is a basic built-in sort weight generator that will set the `sortWeight` based on the configured `priority` on the
entry.  It does this based on the position of the priority concept within the configured allowed priorities concept set
for the `Queue`.  This will be either the Concept Set that is explicitly configured on the Queue, or the default 
Concept Set that is configured via the Global Property.  If there are 3 priorities configured in the Concept Set, the
first priority will result in a `sortWeight` of 0, the second a `sortWeight` of 1, and the third a `sortWeight` of 2.
The primary goal is to provide an easy, built-in way to support a Queue that prioritizes first based on `priority`, 
second based on `startedAt`.  To leverage this option, one should ensure that the Concept Set Members within
their Priority Concept Set are ordered from least to highest priority.

#### Custom Sort Weight Generators
Modules that require this module can define and configure their own custom algorithms.  This would involve:
* Create a Spring Component in your module that implements the `SortWeightGenerator` interface
* Provide a specific bean name for this Component that can be referenced
* Configure the `queue.sortWeightGenerator` global property with this bean name
* See the built-in `BasicPrioritySortWeightGenerator.java` class for a concrete example.

## Documentation

- REST documentation [here](https://rest.openmrs.org/#queue)

## Development

### Building

To build the module, run:

```bash
mvn clean install
```

To skip tests during build:

```bash
mvn clean install -DskipTests
```

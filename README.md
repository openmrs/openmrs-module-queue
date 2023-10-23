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

## Configurations

After installing the queue module, configure the following GPs according to your implementation needs. Note that this is a
necessary step, no defaults are provided.

|Property   | Default value   | Description
|:---|---|---|
|queue.statusConceptSetName | Queue Status | A set of concepts for queue status, i.e Waiting for Service, With Service, and Finished With Service |
|queue.priorityConceptSetName | Queue Priority | A set of queue priority concepts e.g Urgent, Emergency, Not Urgent |
|queue.serviceConceptSetName | Queue Service | A set of queue service concepts. Services offered in a clinic e.g Triage, Consultation, ... |

## Documentation

- REST documentation [here](https://rest.openmrs.org/#queue)

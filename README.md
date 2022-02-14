Openmrs-module-queue
==========================

> Outpatient queue management
    
   > https://talk.openmrs.org/t/backend-support-for-service-delivery-queues-workflows/35247
## Prerequisites
  - Openmrs platform version > 2.3.x
  - Java 8 or higher
  - Webservices rest module -(Always bundled with the platform)

## Configurations
After installing the queue module, configure the following GPs according to your implementation needs. Note that this is a necessary step, no defaults provided.

|Property   | Default value   | Description
|:---|---|---|
|queue.statusConceptSetName | Queue Status | A set of concepts for queue status, i.e Waiting for Service, With Service, and Finished With Service |
|queue.priorityConceptSetName | Queue Priority | A set of queue priority concepts e.g Urgent, Emergency, Not Urgent |
|queue.serviceConceptSetName | Queue Service | A set of queue service concepts. Services offered in a clinic e.g Triage, Consultation, ... |

## Rest docs
> Not ideal place for this, to be migrated later -> https://rest.openmrs.org/#openmrs-rest-api

### Queue
#### Get queue by uuid
```http request
GET /ws/rest/v1/queue/<UUID>
```
| Parameter | Type | Description |
| :--- | :--- | :--- |
| `UUID` | `string` | UUID of cohort queue to be retrieved |
#### Create queue
```http request
POST /ws/rest/v1/queue
```
body
```json
{
    "name": "Triage queue",
    "description": "This triage queue",
    "location": {
        "uuid": "8d6c993e-c2cc-11de-8d13-0010c6dffd0f"
    }
}
```

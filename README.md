Openmrs-module-queue
==========================

> Outpatient queue management

> https://talk.openmrs.org/t/backend-support-for-service-delivery-queues-workflows/35247

## Prerequisites

- Openmrs platform version >= 2.3.x
- Java 8 or higher
- Webservices rest module -(Always bundled with the platform)

## Configurations

After installing the queue module, configure the following GPs according to your implementation needs. Note that this is a
necessary step, no defaults provided.

|Property   | Default value   | Description
|:---|---|---|
|queue.statusConceptSetName | Queue Status | A set of concepts for queue status, i.e Waiting for Service, With Service, and Finished With Service |
|queue.priorityConceptSetName | Queue Priority | A set of queue priority concepts e.g Urgent, Emergency, Not Urgent |
|queue.serviceConceptSetName | Queue Service | A set of queue service concepts. Services offered in a clinic e.g Triage, Consultation, ... |

## Rest docs

> Not the ideal place for this, to be migrated later -> https://rest.openmrs.org/#openmrs-rest-api

### Queue Resource

#### Get queue by uuid

Retrieve a queue by UUID. Returns a `404 Not found` status if the queue(to be retrieved) doesn't exist. If not authenticated,
401 Unauthorized status is returned.

```http request
GET /ws/rest/v1/queue/<UUID>
```

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `UUID` | `string` | UUID of queue to be retrieved |

#### Create queue

Creates new queue using queue object. Some properties aren't allowed to be null which includes;

- name - the name of the queue
- service -
- location - Specifies the location of the queue

```http request
POST /ws/rest/v1/queue
```

Body

```json
{
  "name": "Triage queue",
  "description": "This triage queue description",
  "service": {
    "uuid": "d3db3805-2b90-4330-9064-eb6d42cbf582"
  },
  "location": {
    "uuid": "8d6c993e-c2cc-11de-8d13-0010c6dffd0f"
  }
}
```

#### Update queue

Updates the queue record. Only modifies the properties specified in the request. Returns a `404 Not found` status if the
queue(to be updated) doesn't exist. If not authenticated, 401 Unauthorized status is returned.

```http request
POST /ws/rest/v1/queue/<UUID>
```

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `UUID` | `string` | UUID of queue to be updated |

Body

```json5
{
  "name": "TRIAGE QUEUE (updated)",
  "description": "Queue for patients waiting for triage(updated)"
}
```

#### Void queue

Voids or delete the target queue. Returns a `404 Not found` status if the queue(to be voided) doesn't exist. If not
authenticated, 401 Unauthorized status is returned.

```http request
DELETE /ws/rest/v1/queue/<UUID>?purge=false
```

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `UUID` | `string` | UUID of queue to be voided |
| `purge` | `boolean` | The queue record will be voided unless `purge=true` |

### Queue Entry Resource

#### Get queue entry by uuid

Retrieves queue entry record by UUID. Returns a `404 Not found` status if the queue entry(to be retrieved) doesn't exist. If
not authenticated, 401 Unauthorized status is returned.

```http request
GET /ws/rest/v1/queue/<QueueUUID>/entry/<QueueEntryUUID>
```

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `QueueUUID` | `string` | UUID of the associated queue |
| `QueueEntryUUID` | `string` | UUID of queue entry to be retrieved |

#### Find queue entries by status

Finds queue entries by status. Return empty results if no queue entry with the specified status exists.

```http request
GET /ws/rest/v1/queue/<QueueUUID>/entry?status=Waiting For Service
```
| Parameter | Type | Description |
| :--- | :--- | :--- |
| `QueueUUID` | `string` | UUID of the associated queue |
| `status` | `string` | queue entry status |
#### Create queue entry

Creates queue entry record. If not authenticated, 401 Unauthorized status is returned.

```http request
POST /ws/rest/v1/queue/<queue-UUID>/entry
```

body

```json5
{
  "status": {
    "uuid": "f638838b-f8d3-4679-8d01-6f0088107918"
  },
  "priority": {
    "uuid": "be82478f-6736-4240-a906-578949b1ca02"
  },
  "patient": {
    "uuid": "1296b0dc-440a-11e6-a65c-00e04c680037"
  },
  "priorityComment": "Needs urgent attention",
  "startedAt": "2022-02-10 13:50:54"
}
```

#### Update queue entry

Updates the queue entry record. Only modifies the properties specified in the request. Returns a 404 Not found status if the
queue & queue entry(
to be updated) doesn't exist. If not authenticated, 401 Unauthorized status is returned.

```http request
POST /ws/rest/v1/queue/<QueueUUID>/entry/<QueueEntryUUID>
```

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `QueueUUID` | `string` | UUID of the associated queue |
| `QueueEntryUUID` | `string` | UUID of queue entry to be updated |

Body

```json5
{
  "endedAt": "2022-02-10 16:18:21",
  "priorityComment": "Needs urgent attention (updated)"
}
```

#### Void queue entry

Voids or delete the target queue entry. Returns a `404 Not found` status if the queue entry(to be voided) doesn't exist. If
not authenticated, 401 Unauthorized status is returned.

```http request
DELETE /ws/rest/v1/queue/<QueueUUID>/entry/<QueueEntryUUID>?purge=false
```

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `QueueUUID` | `string` | UUID of queue associated |
| `QueueEntryUUID` | `string` | UUID of queue entry record to be voided |
| `purge` | `boolean` | The queue entry record will be voided unless `purge=true` then deleted |

### Queue Entry Count
####  Get queue entries count
Gets the count of all active queue entries in a given queue.

```http request
GET /ws/rest/v1/queue/<QueueUUID>/count
```
| Parameter | Type | Description |
| :--- | :--- | :--- |
| `QueueUUID` | `string` | UUID of the associated queue |

#### Get queue entries count by status
Gets the count of queue entries record by status
```http request
GET /ws/rest/v1/queue/<QueueUUID>/count?status=Waiting for Service
```
| Parameter | Type | Description |
| :--- | :--- | :--- |
| `QueueUUID` | `string` | UUID of the associated queue |
| `status` | `string` | the status of queue entry |

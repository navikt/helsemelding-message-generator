# helsemelding-message-generator

An application with scheduler(s) for producing fake outgoing messages in dev to the following kafka topic(s):
- `helsemelding.dialog.out.json` (dialog message in JSON format) — used by [helsemelding-outbound-processing-service](https://github.com/navikt/helsemelding-outbound-processing-service)
- `helsemelding.dialog.out.xml` (dialog message in XML format) — **disabled**, kept for reference

## Outgoing dialog messages (JSON)

JSON messages are published to `helsemelding.dialog.out.json` and correspond to the `OutgoingDialogMessage` schema
from `helsemelding-json-schema`.
### Types of JSON dialog messages published

Each message is randomly one of the following:

| Outcome | Probability | Description |
|---|---|---|
| Valid message | 70% | Valid `OutgoingDialogMessage` with valid UUID key |
| Invalid key | 10% | Valid JSON body, but key is not a UUID |
| Invalid JSON | 10% | Key is valid UUID, body is not valid JSON |
| Invalid structure | 10% | Key is valid UUID, body is valid JSON but not `OutgoingDialogMessage` |

Messages include an attachment with 50% probability.

## Outgoing dialog messages (XML) — disabled

XML message generation is currently disabled (`enabled = false` in `application.conf`).
It is implemented using a template (`dialogMessage.xml`) and substituting certain properties with random values.
The XML is published to `helsemelding.dialog.out.xml`.

## Dialog message documentation

Documentation for initial request and follow-up request can be found [here](https://sarepta.helsedir.no/standard/Dialogmelding/1.0;profile=1). 
To read more about other types of messages see [Samhandling i pasientforløp](https://sarepta.helsedir.no/tema/Samhandling%20i%20pasientforl%C3%B8p).

## Incoming messages

Incoming messages are messages that EPJ sends to NAV.
In order to test the incoming message flow, the application also produces incoming messages.
The application produces incoming messages based on the `dialogMessage.xml` template. The difference between the outgoing and incoming messages is that HER-id of the receiver and sender are swapped.
Incoming messages are sent to NHN using EDI Adapter.

### Incoming message with attachment

Incoming messages can contain an attachment (50% chance).
Attachment consists of 1-3 `attachment.xml` nodes.


### Configuration

Relevant configuration for adjusting the frequency and toggling schedulers on/off.

| Property | Description                        | Type     |
|----------|------------------------------------|----------|
| topic    | Topic name                         | String   |
| enabled  | Toggle scheduler on/off            | Boolean  |
| interval | How often scheduler sends messages | Duration |

## Dynamic scheduler configuration

The service exposes endpoints for dynamic configuration of the schedulers:

### Get scheduler status

`GET /scheduler/status`

Returns the current state of all schedulers.

Example request:
```
curl https://<host>/scheduler/status
```

Example response:
```json
{
    "dialogMessages": {
        "enabled": false,
        "interval": "PT5M",
        "lastRunAt": null
    },
    "jsonDialogMessages": {
        "enabled": true,
        "interval": "PT5M",
        "lastRunAt": "2026-08-14T13:00:00Z"
    },
    "incomingMessages": {
        "enabled": true,
        "interval": "PT5M",
        "lastRunAt": "2026-08-14T12:55:00Z"
    }
}
```

### JSON dialog message scheduler

| Endpoint | Description |
|---|---|
| `POST /scheduler/json-dialog-messages/start` | Enable JSON message generation |
| `POST /scheduler/json-dialog-messages/stop` | Disable JSON message generation |
| `POST /scheduler/json-dialog-messages/interval/{seconds}` | Update interval (positive integer) |

### XML dialog message scheduler

| Endpoint | Description |
|---|---|
| `POST /scheduler/dialog-messages/start` | Enable XML message generation |
| `POST /scheduler/dialog-messages/stop` | Disable XML message generation |
| `POST /scheduler/dialog-messages/interval/{seconds}` | Update interval (positive integer) |

### Incoming message scheduler

| Endpoint | Description |
|---|---|
| `POST /scheduler/incoming-messages/start` | Enable incoming message generation |
| `POST /scheduler/incoming-messages/stop` | Disable incoming message generation |
| `POST /scheduler/incoming-messages/interval/{seconds}` | Update interval (positive integer) |

## Generating messages on demand

The service exposes endpoints for generating messages on demand.
You can specify the number of messages using the `count` query parameter (default: 1, max: 100).
Messages are generated with 1 second interval.

| Endpoint | Description |
|---|---|
| `GET /generate/json-dialog-messages?count={count}` | Generate JSON dialog messages |
| `GET /generate/dialog-messages?count={count}` | Generate XML dialog messages |
| `GET /generate/incoming-messages?count={count}` | Generate incoming messages |


# helsemelding-message-generator

An application with scheduler(s) for producing fake outgoing messages in dev to the following kafka topic(s)
(used by [helsemelding-state-service](https://github.com/navikt/helsemelding-state-service)):
- helsemelding.dialog.out.xml (dialog message)

Implemented by using a template (`dialogMessage.xml`) and substituting certain properties with random values from 
the following files:
- `messages.txt`
- `names.txt`

## Types of dialog messages published

To determine what kind of dialog message is published, a random number between 1 and 11 is generated which then 
corresponds to one the following messages:

| Outcome                               | Trigger condition  | Description                                              |
|---------------------------------------|--------------------|----------------------------------------------------------|
| Single valid dialog message           | number between 1–9 | A valid message                                          |
| Single invalid dialog message         | number = 10        | Invalid **record key**: (`null`, `""`, or `"1234-abcd"`) |
| Two valid dialog messages (duplicate) | number = 11        | Same **record key** used twice                           |

## Dialog message documentation

Documentation for initial request and follow-up request can be found [here](https://sarepta.helsedir.no/standard/Dialogmelding/1.0;profile=1). 
To read more about other types of messages see [Samhandling i pasientforløp](https://sarepta.helsedir.no/tema/Samhandling%20i%20pasientforl%C3%B8p).

### Initial request example

Example of an initial request can be found [here](https://git.sarepta.ehelse.no/publisert/standarder/raw/master/eksempel/Dialogmelding/Dialogmelding-v1-0/Dialogmelding_foresporsel_PLO_v1-0.xml)
which is the basis for `dialogMessage.xml`.

## Incoming messages

Incoming messages are messages that EPJ sends to NAV.
In order to test the incoming message flow, the application also produces incoming messages.
The application produces incoming messages based on the `dialogMessage.xml` template. The difference between the outgoing and incoming messages is that HER-id of the receiver and sender are swapped.
Incoming messages are sent to NHN using EDI Adapter.

## Local development

Running the application locally:
1. Replace the usage of `dialogMessagePublisher` with a fake one. 
   See [Replacing dialogMessagePublisher with a fake](#Replacing-dialogMessagePublisher-with-a-fake) for more details.
2. Run the application (typically by running the `App` class in your IDE).

### Configuration

Relevant configuration for adjusting the frequency and toggle scheduler for dialog message on/off.

| Property | Description                        | Type     |
|----------|------------------------------------|----------|
| topic    | Topic name                         | String   |
| enabled  | Toggle scheduler on/off            | Boolean  |
| interval | How often scheduler sends messages | Duration |

### Replacing dialogMessagePublisher with a fake

To run this locally (meaning without actually publishing to kafka topic) change the following in App.kt:
```kotlin
val dialogMessageProcessor = DialogMessageProcessor(dialogMessagePublisher)
```

to use `FakeDialogMessagePublisher` instead:
```kotlin
val dialogMessageProcessor = DialogMessageProcessor(FakeDialogMessagePublisher())
```

## Dynamic scheduler configuration 

The service exposes endpoints for dynamic configuration of the scheduler:

#### Get scheduler status

`GET /scheduler/status`

Returns the current state of all schedulers.

Example request:
```
curl https://<host>/scheduler/status
```

Example response:
```
{
    "dialogMessages": {
        "enabled": true,
        "interval": "PT3M",
        "lastRunAt": "2026-04-24T10:15:00Z"
    },
    "incomingMessages": {
        "enabled": true,
        "interval": "PT4M",
        "lastRunAt": "2026-04-24T10:14:30Z"
    }
}
```

#### Stop dialog message scheduler

`POST /scheduler/dialog-messages/stop`

Disables generation of dialog messages.

```
curl -X POST https://<host>/scheduler/dialog-messages/stop
```

#### Start dialog message scheduler

`POST /scheduler/dialog-messages/start`

Enables generation of dialog messages.

```
curl -X POST https://<host>/scheduler/dialog-messages/start
```

#### Update dialog message interval

`POST /scheduler/dialog-messages/interval/{seconds}`

Updates the interval between generations of dialog messages. 
`{seconds}` must be a positive integer

```
curl -X POST https://<host>/scheduler/dialog-messages/interval/300
```

#### Stop incoming message scheduler

`POST /scheduler/incoming-messages/stop`

Disables generation of incoming messages.

```
curl -X POST https://<host>/scheduler/incoming-messages/stop
```

#### Start incoming message scheduler

`POST /scheduler/incoming-messages/start`

Enables generation of incoming messages.

```
curl -X POST https://<host>/scheduler/incoming-messages/start
```

#### Update incoming message interval

`POST /scheduler/incoming-messages/interval/{seconds}`

Updates the interval between generations of incoming messages. 
`{seconds}` must be a positive integer

```
curl -X POST https://<host>/scheduler/incoming-messages/interval/30
```

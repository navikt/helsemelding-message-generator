# helsemelding-message-generator

An application with scheduler(s) for producing fake outgoing messages in dev to the following kafka topic(s)
(used by [helsemelding-state-service](https://github.com/navikt/helsemelding-state-service)):
- helsemelding.dialog.out.xml (dialog message)

Implemented by using a template (`dialogMessage.xml`) and substituting certain properties with random values from 
the following files:
- `messages.txt`
- `names.txt`

## Dialog message documentation

Documentation for initial request and follow-up request can be found [here](https://sarepta.helsedir.no/standard/Dialogmelding/1.0;profile=1). 
To read more about other types of messages see [Samhandling i pasientforløp](https://sarepta.helsedir.no/tema/Samhandling%20i%20pasientforl%C3%B8p).

### Initial request example

Example of an initial request can be found [here](https://git.sarepta.ehelse.no/publisert/standarder/raw/master/eksempel/Dialogmelding/Dialogmelding-v1-0/Dialogmelding_foresporsel_PLO_v1-0.xml)
which is the basis for `dialogMessage.xml`.

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

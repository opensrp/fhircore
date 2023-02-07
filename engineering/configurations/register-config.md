# Register configuration

Registers are the entry point to FHIRCore application. Typically this is a list used to displayed the configured [FHIR resources](https://www.hl7.org/FHIR/resourcelist.html). Clicking on a register item directs the user to the configured profile.

:::info For every register in the application there should at least be one profile configuration. Similar registers can re-use the same profile configuration. :::

## Sample JSON

{}

## Config properties

| Property   | Description                           | Required |   Default  |
| ---------- | ------------------------------------- | :------: | :--------: |
| appId      | Unique identifier for the application |    Yes   |            |
| configType | Type of configuration                 |    Yes   | `register` |

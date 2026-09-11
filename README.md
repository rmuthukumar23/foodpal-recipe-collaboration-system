# FoodPal

FoodPal is a multi-client recipe manager built as a team software engineering project at TU Delft. It combines a JavaFX desktop client with a Spring Boot server so multiple clients can work with the same recipe library and receive live updates when recipes change.

I worked mainly on the client side and feature integration, including the shopping-list flow, labels and filtering, timer functionality, WebSocket-driven refresh behaviour, favorites handling, UI fixes, and tests.

## What it does

- create, edit, clone, delete and search recipes
- share recipes through a Spring Boot REST backend
- push recipe changes to connected clients with WebSocket/STOMP
- store recipes and global ingredients in H2 through Spring Data JPA
- manage nutritional values and estimate recipe caloric density
- filter recipes by labels, language and other metadata
- build and export shopping lists
- save local favorites and UI preferences
- switch the interface between English, Dutch and German

## Stack

Java, JavaFX, Spring Boot, Spring Data JPA, H2, WebSocket/STOMP, Maven, JUnit, Mockito and TestFX.

## Architecture

```text
JavaFX client
    |
    | REST / JSON
    v
Spring Boot server ---- Spring Data JPA ---- H2
    |
    +---- WebSocket / STOMP ----> connected clients
```

The project is split into three Maven modules:

- `client` contains the JavaFX application and client-side utilities
- `commons` contains the shared recipe domain model
- `server` contains the REST API, persistence layer and WebSocket configuration

## Running locally

Requirements: JDK 25 and Maven, or the included Maven wrapper.

Build the project:

```bash
./mvnw clean install
```

Start the server:

```bash
./mvnw -pl server spring-boot:run
```

Then start the client in another terminal:

```bash
./mvnw -pl client javafx:run
```

To see the live-sync behaviour, run the client twice and edit a recipe in one window.

## Project history

The project was originally developed in a private university repository using feature branches, merge requests and CI. I copied it to GitHub after the project had finished so I could keep a public portfolio version. The original development history is therefore not reproduced in this repository.

This was a team project. The source retains the original licence and attribution where applicable.

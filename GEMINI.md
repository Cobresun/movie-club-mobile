
# Movie Club Mobile App

This document outlines the architecture of the Movie Club mobile application, a Kotlin Multiplatform project for Android and iOS. It follows the principles of Clean Architecture, ensuring a separation of concerns between the UI, business logic, and data layers.

## Project Structure

The project is organized into modules, each representing a specific feature or layer of the application.

### High-Level Overview

- **`composeApp`**: The main module containing the shared code for both Android and iOS.
  - **`commonMain`**: The heart of the application, containing the shared business logic, UI, and domain models.
  - **`androidMain`**: Android-specific code, including the `MainActivity`, `MainApplication`, and platform-specific dependency injection modules.
  - **`iosMain`**: iOS-specific code, including the `MainViewController` and platform-specific dependency injection modules.
- **`iosApp`**: The iOS application entry point.

### Architectural Layers

The `commonMain` module is further divided into layers, following the principles of Clean Architecture:

- **`presentation`**: This layer is responsible for the UI and user interaction. It contains:
  - **`ViewModel`s**: These classes hold the UI state and handle user events.
  - **`Screen`s**: These are the Composable functions that define the UI.
  - **`components`**: Reusable UI components.
- **`domain`**: This layer contains the core business logic of the application. It includes:
  - **`Repository` interfaces**: These define the contracts for accessing data.
  - **Use cases**: These classes encapsulate specific business operations.
  - **Domain models**: These are the plain data classes that represent the core entities of the application.
- **`data`**: This layer is responsible for providing data to the domain layer. It contains:
  - **`Repository` implementations**: These classes implement the `Repository` interfaces defined in the domain layer.
  - **Data sources**: These classes are responsible for fetching data from the network or a local database.
  - **DTOs (Data Transfer Objects)**: These are the data classes that represent the data coming from the network or database.
  - **Mappers**: These classes are responsible for mapping DTOs to domain models.

### Feature Modules

The application is also organized by feature, with each feature having its own package within the `commonMain` module. Each feature package contains the `presentation`, `domain`, and `data` layers for that specific feature.

- **`auth`**: Handles user authentication.
- **`club`**: Manages movie club information.
- **`core`**: Contains shared code used across different features, such as data handling, domain models, and UI components.
- **`member`**: Manages club members.
- **`reviews`**: Handles movie reviews.
- **`tmdb`**: Interacts with The Movie Database (TMDB) API.
- **`watchlist`**: Manages the user's watchlist.

## Dependencies

- **Compose Multiplatform**: For building the UI for both Android and iOS from a single codebase.
- **Koin**: For dependency injection.
- **Ktor**: For making network requests.
- **DataStore**: For storing key-value pairs.

## Getting Started

To get started with the project, you'll need to have the following installed:

- Android Studio
- Xcode
- Kotlin Multiplatform Mobile plugin

Once you have the necessary tools installed, you can open the project in Android Studio and run it on either an Android or iOS device.

## How to Contribute

When contributing to the project, please follow the existing architecture and coding style. Here are some guidelines:

- **Create a new feature module** for each new feature.
- **Follow the Clean Architecture principles** by separating the UI, business logic, and data layers.
- **Use Koin for dependency injection**.
- **Write unit tests** for your code.
- **Keep the code in `commonMain`** as much as possible, and only use platform-specific code when necessary.

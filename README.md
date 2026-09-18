# T88Controller

A Kotlin Multiplatform (Compose Multiplatform) application for controlling a hardware device over a network connection, built end-to-end during an R&D internship - from architecture design to production release.

## Overview

T88Controller lets a mobile client drive and monitor a hardware controller in real time. The project's focus is translating physical/electrical constraints into reliable application logic, on top of a single shared Kotlin codebase for Android and iOS.

## Key features

- Asynchronous TCP sockets for a persistent, bidirectional connection between the app and the controller.
- Custom protocol parser that decodes hexadecimal frames, including bitwise masking to safely extract packed status/command bytes.
- Kotlin Multiplatform + Compose Multiplatform for a shared UI and business-logic layer across Android and iOS.
- CI/CD via Codemagic, building Android (APK) and iOS (IPA) artifacts on virtualized macOS environments.

## Tech stack

Kotlin, Kotlin Multiplatform, Compose Multiplatform, Coroutines, TCP/IP sockets

## Project structure

- androidApp/ - Android application module
- iosApp/ - iOS application module
- shared/ - shared business logic, networking, and protocol parsing

## Getting started

Clone the repository, then open it in Android Studio (with the Kotlin Multiplatform plugin) to build and run the Android target, or open iosApp/ in Xcode for the iOS target.

Android app: ./gradlew :androidApp:assembleDebug

iOS app: open the /iosApp directory in Xcode and run it from there.

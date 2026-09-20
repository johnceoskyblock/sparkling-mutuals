# Sparkling Mutuals

A Fabric client-side mod for Hypixel SkyBlock Safari.

Sparkling Mutuals helps players find mutual sparkling critters between party members and look up Safari ticket counts.

## Features

### Mutual Sparkling Critters

Use:

`!mutual`

or:

`!mutuals`

The mod finds sparkling critters that every party member has discovered and reports the mutual Safari timesave critters.

### Safari Tickets

Use:

`!ticket <IGN>`

or:

`!tickets <IGN>`

Example:

`!tickets JohnCEO`

Returns:

`JohnCEO: Basic (123), Economy (45), Premium (17), First-Class (3)`

## Requirements

- Minecraft 26.1.2
- Fabric Loader
- Fabric API
- Fabric Language Kotlin
- HMAPI
- A Hypixel API key

## API Key Setup

After installing the mod, use:

`/apiKey YOUR_API_KEY`

Your API key is stored locally and is not included with the mod.

## Installation

1. Install Fabric for Minecraft 26.1.2.
2. Install the required dependencies.
3. Place `sparkling-mutuals-x.x.x.jar` in your `mods` folder.
4. Launch Minecraft.
5. Set your Hypixel API key using `/apiKey`.

## Building From Source

Clone the repository and run:

`./gradlew build`

The compiled mod will be located in:

`build/libs/`
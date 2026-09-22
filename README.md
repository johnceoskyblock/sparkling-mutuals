# Sparkling Mutuals

A Fabric client-side mod for Hypixel SkyBlock Safari.

Sparkling Mutuals helps players find mutual sparkling critters between party members, check which Safari timesave sparkling critters a player is missing, and look up Safari ticket counts.

## Features

### Mutual Sparkling Critters

Use:

`!mutual`

or:

`!mutuals`

The mod finds sparkling critters that **every party member** has discovered and reports the mutual Safari timesave critters.

### Missing Timesave Sparkling Critters

Use:

`!missing <IGN>`

Example:

`!missing JohnCEO`

The mod checks the player's discovered sparkling critters and reports which Safari timesave sparkling critters they are missing.

The command checks:

* Rockmite
* Snoozle
* Gemzie
* Honeybug
* Gazer
* Gimmiegold
* Doomspiral
* Wumpa
* All Birds — Bluebird + Parakeet + Macaw

Example response:

`JohnCEO: Rockmite, Gemzie, All Birds, Wumpa`

If the player has all timesave sparkling critters, the response will be:

`JohnCEO: None`

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

* Minecraft 26.1.2
* Fabric Loader
* Fabric API
* Fabric Language Kotlin
* HMAPI
* A Hypixel API key

## API Key Setup

Sparkling Mutuals requires a Hypixel API key to access SkyBlock profile data.

### 1. Get your Hypixel API key

Go to:

https://developer.hypixel.net

Log in using your **Hypixel-linked Minecraft account**.

Once logged in, find your API key and click **Regenerate API Key**.

Copy the newly generated API key to your clipboard.

### 2. Enter your API key in Minecraft

After launching Minecraft with Sparkling Mutuals installed, use:

`/apikey YOUR_API_KEY`

For example:

`/apikey abc123...`

The command is case-insensitive, so these also work:

`/apiKey YOUR_API_KEY`

`/APIKEY YOUR_API_KEY`

### 3. Update your API key every 6 hours

**Hypixel API keys expire after 6 hours.** You must regenerate your API key and update it in Sparkling Mutuals every 6 hours to continue using features that require the Hypixel API.

When your key expires:

1. Go to https://developer.hypixel.net
2. Log in with your Hypixel-linked account.
3. Click **Regenerate API Key**.
4. Copy the new key to your clipboard.
5. Run `/apikey YOUR_NEW_API_KEY` in Minecraft.

You do **not** need to reinstall the mod when your API key expires.

Your API key is stored locally in your Minecraft configuration and is not included with the mod or published with the source code.

## Installation

1. Install Fabric for Minecraft 26.1.2.
2. Install the required dependencies.
3. Place `sparkling-mutuals-x.x.x.jar` in your Minecraft `mods` folder.
4. Launch Minecraft.
5. Get your Hypixel API key from https://developer.hypixel.net.
6. Copy the key to your clipboard.
7. Enter `/apikey YOUR_API_KEY` in Minecraft.
8. Sparkling Mutuals is now ready to use.

## Commands

| Command          | Description                                                 |
| ---------------- | ----------------------------------------------------------- |
| `/apikey <key>`  | Set or update your Hypixel API key                          |
| `!mutual`        | Find mutual timesave sparkling critters among party members |
| `!mutuals`       | Same as `!mutual`                                           |
| `!missing <IGN>` | Find which timesave sparkling critters a player is missing  |
| `!ticket <IGN>`  | Look up a player's Safari ticket counts                     |
| `!tickets <IGN>` | Same as `!ticket`                                           |

## Building From Source

Clone the repository and run:

`./gradlew build`

The compiled mod will be located in:

`build/libs/`

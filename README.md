# Sparkling Mutuals

A Fabric client-side mod for Hypixel SkyBlock Safari.

Sparkling Mutuals helps players find mutual sparkling critters between party members, check which Safari timesave sparkling critters a player is missing, look up Safari ticket counts, and track Safari contests.

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

### Safari Contest HUD

Use:

`/sparkling gui`

This opens the Safari Contest HUD editor.

The HUD displays:

* Current contest time remaining
* Current contest tier and score
* Tier-specific colors matching the SkyBlock contest brackets

#### Moving the HUD

1. Run `/sparkling gui`.
2. Left-click and drag the HUD to move it.
3. Release the mouse button when it is in the desired position.
4. Close the GUI to save the position.

#### Resizing the HUD

1. Run `/sparkling gui`.
2. Hover your mouse over the HUD.
3. Scroll up to increase its size.
4. Scroll down to decrease its size.
5. Close the GUI to save the size.

The HUD can be resized between 50% and 200%.

The HUD position and size are saved automatically and persist between game sessions.

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

Once logged in, click **Create API Key**.

Copy the newly generated API key to your clipboard.

### 2. Enter your API key in Minecraft

After launching Minecraft with Sparkling Mutuals installed, use:

`/apikey YOUR_API_KEY`

For example:

`/apikey abc123...`

The command is case-insensitive, so these also work:

`/apiKey YOUR_API_KEY`

`/APIKEY YOUR_API_KEY`

### 3. Update your API key whenever it expires

**Hypixel API keys expire after a certain amount of time.** You must regenerate your API key and update it in Sparkling Mutuals to continue using features that require the Hypixel API.

When your key expires:

1. Go to https://developer.hypixel.net
2. Log in with your Hypixel-linked account.
3. Click **Regenerate API Key**.
4. Copy the new key to your clipboard.
5. Run `/apikey YOUR_NEW_API_KEY` in Minecraft.

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
| `/sparkling gui` | Open the Safari Contest HUD editor                          |
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
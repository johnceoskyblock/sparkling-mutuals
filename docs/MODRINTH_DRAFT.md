# Sparkling Mutuals for Modrinth

The support inquiry and project copy below are drafts. Use the source link and any additional disclosures Modrinth requests. Replace the listing's relative party-sync link with its approved public URL before submission.

## Support inquiry

Hello Modrinth team,

We're preparing to submit Sparkling Mutuals, a client-side Fabric mod for Hypixel SkyBlock's Safari. The proposed listing would distribute only the Standard build for Minecraft 26.1.2, 26.2 and 26.3. Standard keeps local line-of-sight markers, run tracking, party progress sync, HUDs, and history. Its build excludes gameplay automation, through-terrain rendering, hidden-discovery alerts, and the custom updater.

We maintain Standard and the Full builds in one repository. The Full builds are distributed separately on GitHub and include automation and through-terrain features. Our release checks verify what each Standard JAR contains, and we intend to upload only verified Standard JARs after approval. Modrinth publishing is not enabled yet. Can we use the shared repository as the Modrinth source link, or should we provide a Standard-only source location? Please let us know what disclosures or source review you require.

Standard connects to external services for Safari party progress and SkyBlock lookups. Party sync sends Minecraft UUIDs for the current Safari party, the server identifier, and bounded Safari objective and quest-item counts. We plan to describe these connections on the listing.

Thanks,
The Sparkling Mutuals maintainers

## Project listing

- **Name** Sparkling Mutuals
- **Summary** Safari run tracking, local critter highlights, party progress, and history for Hypixel SkyBlock.
- **Platform** Fabric client for Minecraft 26.1.2, 26.2, and 26.3. Use Java 25, Fabric API, and Fabric Language Kotlin.

### Safari tracking and HUDs

Track Unique, Full Clear, and Sparkle runs. See Safari objectives, splits, capture counts, and Miria contest reminders in configurable HUDs. Review local run history, personal bests, and profit estimates. Local highlights and sparkling alerts are limited to sightings the client can currently see.

### Party progress and network use

Sparkling collection commands use SkyBlock profile data. Depending on your settings, the mod uses its hosted profile lookup service or your configured Hypixel API connection. Name resolution uses Mojang's public profile API where needed.

Safari party sync shares objectives with compatible clients in the current room. It sends the local player's Minecraft UUID for authentication. The sync service uses the confirmed party's public Minecraft UUIDs and the Safari server identifier to match the room. It exchanges bounded Safari progress, discoveries, and relevant held quest-item counts. See the [party sync documentation](PARTY_SYNC.md) for the service and protocol details.

### Installation and updates

Use the Standard file matching your exact Minecraft version. Close Minecraft and replace the previous Sparkling Mutuals JAR before launching. Standard uses `config/sparkling-mutuals` and imports missing saves from the previous Standard folder on first launch. Future Standard updates will come through Modrinth.

Review Hypixel's modification rules before connecting. [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications)

<div align="center">

# Pulsify
Lightweight server monitoring SDK for Minecraft. Captures errors, tracks player events and custom metrics, and ships them to the Pulsify backend — across Paper, BungeeCord, and Velocity.

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/plugin/pulsify)
[![Chat on Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/qNyybSSPm5)

</div>

## ⚙️ Features
- 🐛 Automatic error capture via Log4j2 or JUL with stack traces
- 📊 Custom metrics with optional label support
- 👤 Player join/quit event tracking
- 💓 Periodic server heartbeats with server and plugin info
- 📦 Batched event queue with configurable flush intervals
- 🔌 Multi-platform: Paper, BungeeCord, Velocity (mod support coming soon)

## 🚀 Quick Start
Initialize the client once during plugin startup:

```java
StatClient client = StatClient.builder()
    .dsn("your-dsn-here")
    .autoCollectErrors(true)
    .build();
```

Capture errors and metrics manually:

```java
// Report a caught exception
client.error("my-plugin", exception);

// Send a custom metric
client.metric("tps", server.getTPS()[0]);
```

Close the client on shutdown to flush pending events:

```java
client.close();
```

## 📦 Building
To build Pulsify, follow these steps (Make sure you have **JDK 17 or higher**):

```shell
./gradlew build
```
- Platform jars will be located at `platforms/<platform>/build/libs/`.

## ⚖️ License ![Static Badge](https://img.shields.io/badge/license-GPL_3.0-lightgreen)

Pulsify SDK is licensed under the GNU General Public License v3.0. You can find the license [here](LICENSE).

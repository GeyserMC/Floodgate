# Minekube Connect - Plugin

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/master/badge/icon)](https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/master/)
[![Discord](https://img.shields.io/discord/633708750032863232.svg?color=%237289da&label=discord)](https://minekube.com/discord)
[![HitCount](https://hits.dwyl.com/minekube/connect-java.svg)](http://hits.dwyl.com/minekube/connect-java)

Minekube Connect allows you to connect any Minecraft server, whether online mode, public, behind
your protected home network or anywhere else in the world, with our highly available, performant and
low latency edge proxies network nearest to you.

Please refer to https://github.com/minekube/connect for more documentation. A documentation website
is following.

**Special thanks goes to the [GeyserMC](https://github.com/GeyserMC) developers for their Floodgate
and GeyserMC open source projects.** This repository forks Floodgate and only reuses its phenomenal
project layout for our plugin as well as the very similar internal player connection injection
methods applied. Note that our plugin is completely different from Floodgate and Geyser plugins as
it differs in functionality and should work alongside those as we have refactored our plugin to work
isolated from the upstream.

## Working setups

The Connect plugin supports the following platform settings.

- PaperMC/Spigot
    - ✔️️ No forwarding + Online mode
    - ✔️ No forwarding + Offline mode
    - ✔️ Velocity forwarding + Online/Offline mode
    - ✔️ Bungee forwarding + Offline mode
    - ❌ Bungee forwarding + Online mode
- Velocity
    - ✔️ Velocity forwarding (aka modern) + Online/Offline mode
        - ❌ Can't connect to Velocity enabled PaperMC server through Velocity proxy
    - ✔️ Bungee forwarding (aka legacy) + Offline/Offline mode
    - ✔️ None forwarding + Online/Offline mode
- Bungee
    - ✔️ Bungee forwarding + Offline mode
    - ✔️ Bungee forwarding + Online mode

You can install the Connect plugin on any of the above platforms. The plugin will automatically
detect the platform and will configure itself accordingly.

You can even install Connect on Velocity or BungeeCord proxy and the Connect services treat it as a
normal Minecraft server. This allows you to use your existing proxy setup and still use the Connect
services. This ultimately allows you to add your Minecraft networks to the global Connect network.

You can also install the Connect plugin on your Spigot/PaperMc servers and still join them from your
own proxies as well as through the Connect network.

You don't need to use own proxies since the Connect network already works like global shared proxy
where every Minecraft server/proxy can connect to.

There will be public APIs available for you to manage your connected endpoints and players like
sending a players a message, moving them between your servers and so on.
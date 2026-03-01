# BanSentinel

A simple, lightweight, and high-performance ban management plugin for Spigot and Paper servers. 

I built this because I wanted a punishment system that was easy to use, didn't lag the server, and actually worked properly with the newer 1.21 API versions.

## ✨ Features
* **Fast & Lightweight:** Uses .yml files for storage, so no complex database setup is needed.
* **Smart Banning:** Supports both UUID (account) and IP-based bans.
* **Temp-bans:** Easy time formats like `1d` (day), `1h` (hour), or `30m` (minutes).
* **Pardon System:** Clean unban commands that actually clear the player's status immediately.
* **Formatted Kick Screens:** Fully supports color codes in your ban reasons and appeal links.
* **History Tracking:** Automatically moves expired bans to a history folder for your records.

## 🚀 Installation
1. Download the latest `BanSentinel.jar` from the releases tab.
2. Drop it into your server's `plugins` folder.
3. Restart your server.
4. Edit the `config.yml` to add your own Discord appeal link.

## 💻 Commands & Permissions
* `/ban <player> [time] [reason] [-s]` — Ban a player (add -s for silent).
* `/ipban <player> [time] [reason]` — Ban a player's IP address.
* `/unban <player/*>` — Unban a specific player or use * to clear everyone.
* `/check <player>` — See a player's punishment history.
* `/banreload` — Reload the config file.

**Permission:** `simpleban.admin` (Default: OP)

## 🛠️ Build Information
This project is built using Java 17+ and is compatible with the latest Spigot/Paper API. Feel free to fork the repo or open an issue if you find a bug!

---
Made by Deb. If you find this useful, leave a star on GitHub!

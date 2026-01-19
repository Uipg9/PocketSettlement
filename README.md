# 🏰 Pocket Settlement: The Governor's Desk

> A cozy, "Vanilla Plus" colony simulator that plays entirely within a beautiful, code-rendered GUI. No placing blocks in the world—you manage the settlement from your desk.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green.svg)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📖 Overview

**Pocket Settlement** is a Fabric mod for Minecraft 1.21.11 that adds a complete colony management experience through an elegant, desk-themed GUI. Instead of cluttering your world with bulky building systems, you manage everything from a parchment-and-ink interface that feels like you're sitting at the governor's desk.

### ✨ Key Features

- **📋 7×7 Settlement Grid**: Place and manage buildings in a compact grid layout
- **👥 Citizen System**: Recruit, train, and assign workers to maximize efficiency
- **🏗️ 11 Building Types**: From Greenhouses to Guard Towers, each with unique purposes
- **📜 Daily Contracts**: Fulfill merchant orders to earn coins
- **🔬 Technology Tree**: 25+ research nodes across Industry, Civics, and Logistics
- **📦 Virtual Stockpile**: Store and withdraw resources without cluttering your inventory

---

## 🎮 Getting Started

### Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11
2. Download and install [Fabric API](https://modrinth.com/mod/fabric-api) (0.141.1+1.21.11)
3. Download the latest release of Pocket Settlement
4. Place the `.jar` file in your `mods` folder
5. Launch Minecraft!

### Opening the Desk

Press **G** (configurable) to open the Governor's Desk interface. From here, you can access all settlement management features.

---

## 🏠 Buildings

| Building | Purpose | Workers |
|----------|---------|---------|
| 🏡 House | Increases population cap | - |
| 🌾 Greenhouse | Produces crops (wheat, carrots, potatoes) | Farmer |
| ⛏️ Quarry | Mines stone, coal, iron, gold | Miner |
| 🪵 Lumber Yard | Harvests logs and saplings | Lumberjack |
| 🐄 Mob Barn | Raises animals for leather, wool, food | Rancher |
| 🏪 Market | Enables contracts, generates coins | Merchant |
| 🏦 Bank | Passive coin generation, boosts income | - |
| 📚 Academy | Train citizens, unlock advanced jobs | Scholar |
| ⚔️ Guard Tower | Defense, unlocks Guard job | Guard |
| 🏛️ Town Hall | Central building, boosts all production | - |

### Adjacency Bonuses

Buildings placed next to complementary structures receive bonuses:
- **Greenhouse + House**: +10% crop yield
- **Quarry + Academy**: +15% ore extraction
- **Lumber Yard + House**: +10% wood production
- **Guard Tower + Town Hall**: +20% defense

---

## 👥 Citizens

Citizens are the heart of your settlement. Each citizen has:

- **Job**: Determines what buildings they can work in
- **Level (1-5)**: Higher levels = better efficiency
- **XP**: Gained through work, leads to level-ups
- **Happiness**: Affects productivity (influenced by housing and services)

### Jobs

| Job | Building | Produces |
|-----|----------|----------|
| 🌾 Farmer | Greenhouse | Crops |
| ⛏️ Miner | Quarry | Ores & Stone |
| 🪵 Lumberjack | Lumber Yard | Wood & Saplings |
| 🐄 Rancher | Mob Barn | Animal Products |
| 💰 Merchant | Market | Coins |
| 📖 Scholar | Academy | XP for others |
| ⚔️ Guard | Guard Tower | Defense |

---

## 📜 Contracts

Every morning (tick 0), the Merchant generates 3 new contracts. Each contract:
- Requests specific resources (e.g., 32 wheat)
- Offers coin rewards upon completion
- Expires at the end of the day

Fulfill contracts by delivering resources from your stockpile!

---

## 🔬 Technology Tree

Research technologies to unlock new buildings and upgrades. Three branches:

### 🔧 Industry Branch
- Farming I-III: Better crop yields
- Mining I-III: Access deeper ores
- Forestry I-III: Faster wood production
- Ranching I-III: More animal products

### 🏛️ Civics Branch
- Housing I-III: More citizens
- Commerce I-III: Better contract rewards
- Education I-III: Faster training
- Defense I-III: Stronger guards

### 📦 Logistics Branch
- Storage I-III: Larger stockpile
- Automation I-III: Background production bonuses
- Contracts I-III: More daily contracts

---

## 💻 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/settlement info` | All | View settlement overview |
| `/settlement citizens` | All | List all citizens |
| `/settlement buildings` | All | Show building grid |
| `/settlement tech` | All | View technology progress |
| `/settlement stockpile` | All | List stored resources |
| `/settlement give coins <amount>` | OP | Add coins (debug) |
| `/settlement give influence <amount>` | OP | Add influence (debug) |
| `/settlement unlock <tech>` | OP | Force unlock technology |
| `/settlement reset` | OP | Reset entire settlement |

---

## 🎨 Design Philosophy

### "Native Luxury"

The UI is designed with a **parchment-and-ink aesthetic**:
- Cream-colored backgrounds resembling aged parchment
- Leather-brown borders evoking a well-worn desk
- Ink-black text with red and green accents
- Gold highlights for coins and rewards

### No World Blocks

Unlike many colony mods, Pocket Settlement keeps everything contained in the GUI:
- No lag from rendering hundreds of building blocks
- No chunk-loading issues
- Clean, uncluttered worlds
- Portable save data

---

## 📁 Project Structure

```
src/main/java/com/uipg9/pocketsettlement/
├── PocketSettlement.java          # Main mod initializer
├── PocketSettlementClient.java    # Client-side (keybindings)
├── commands/
│   └── SettlementCommand.java     # Debug commands
├── data/
│   ├── Building.java              # Building data model
│   ├── BuildingType.java          # Building type enum
│   ├── Citizen.java               # Citizen data model
│   ├── CitizenJob.java            # Job enum
│   ├── Contract.java              # Daily contract model
│   ├── Stockpile.java             # Resource storage
│   ├── TechTree.java              # Technology tree
│   └── SettlementState.java       # Persistent save data
├── gui/
│   ├── DeskScreen.java            # Main menu
│   ├── GridScreen.java            # Building grid
│   ├── BuildMenuScreen.java       # Build selection
│   ├── ManageBuildingScreen.java  # Upgrade/demolish
│   ├── AssignWorkerScreen.java    # Worker assignment
│   ├── GreenhouseScreen.java      # Farming interface
│   ├── QuarryScreen.java          # Mining interface
│   ├── LumberYardScreen.java      # Forestry interface
│   ├── MobBarnScreen.java         # Ranching interface
│   ├── CitizenScreen.java         # Citizen management
│   ├── StockpileScreen.java       # Resource withdrawal
│   ├── ContractScreen.java        # Contract fulfillment
│   └── TechScreen.java            # Technology research
├── network/
│   └── SettlementNetworking.java  # Packet handling
└── tick/
    └── SettlementTickManager.java # Simulation processing
```

---

## 🧪 Testing Checklist

After installation, verify these features work:

- [ ] Press G to open Governor's Desk
- [ ] Navigate to Grid view
- [ ] Place a House building
- [ ] Recruit a citizen
- [ ] Place a Greenhouse
- [ ] Assign the citizen to the Greenhouse
- [ ] Wait for production (resources in stockpile)
- [ ] Open Stockpile and withdraw items
- [ ] Check `/settlement info` command
- [ ] Research a technology

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/Uipg9/PocketSettlement.git
cd PocketSettlement

# Build the mod
./gradlew build

# Run the client for testing
./gradlew runClient
```

### Requirements
- Java 21
- Gradle 8.4+

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Uipg9**

---

## 🙏 Acknowledgments

- [Fabric](https://fabricmc.net/) - Mod loader
- [SGUI](https://github.com/Patbox/sgui) - Server-side GUI library
- The Minecraft modding community

---

## 📌 Version History

### v1.0.0
- Initial release
- 7×7 settlement grid
- 11 building types
- Citizen system with 7 jobs
- Technology tree (25+ nodes)
- Daily contract system
- Virtual stockpile with withdrawal

---

**Enjoy managing your pocket-sized settlement!** 🏰✨

# Tutorial Island Bot (TUT)

Fully automated, robust Tutorial Island script for Microbot / RuneLite with zero-restart hot reloading.

## Features
- **End-to-End Automation**: Takes brand-new accounts from character appearance/name selection all the way to Lumbridge mainland.
- **Dynamic Hot Reloading**: Deploy, reload, or stop the bot on the fly using the RuneLite Agent Server without closing or restarting the client.
- **Modernized Tutorial Steps**:
  - Appearance randomization & automatic username generator / suggestion handler.
  - Survival Expert: Shrimps fishing, skills tab, woodcutting, firemaking, and shrimp cooking.
  - Master Chef: Dough mixing, range baking, and pathing.
  - Quest Guide: Quest tab stone and mine cavern ladder descent.
  - Mining & Smithing: Tin/copper mining, furnace smelting, and widget-targeted bronze dagger smithing.
  - Combat Instructor: Equipment equipping, melee giant rat kill, ranged giant rat kill, and ladder ascent.
  - Bank & Account Guide: Bank booth, poll booth with automatic dismissal, account management tab, and door exit.
  - Church / Brother Brace: Prayer & friends tabs and door exit.
  - Magic Instructor: Runes collection, chicken spellcasting, teleport acceptance dialogs, and Lumbridge Home Teleport to mainland.

## Usage

### Deploy & Run
```powershell
.\deploy.ps1 [-Port 8082]
```

### Hot Reload
```powershell
.\reload.ps1 [-Port 8082]
```

### Stop
```powershell
.\stop.ps1 [-Port 8082]
```

# 💥 EzExplosionManager

**EzExplosionManager** gives your server precise control over player damage from major explosion sources while keeping gameplay predictable and configurable.

Built for Bukkit-compatible servers (Spigot/Paper/Folia), this plugin lets you scale explosion damage from:
- End Crystals
- TNT
- TNT Minecarts
- Beds & Respawn Anchors (block explosions)

---

## ✨ What this plugin does

EzExplosionManager listens for explosion damage events and applies your configured multipliers to **player damage only**.

### Supported explosion categories
- **End Crystal** (`blast-control.crystal-scale`)
- **TNT** (`blast-control.tnt-scale`)
- **TNT Minecart** (`blast-control.minecart-scale`)
- **Bed / Respawn Anchor blasts** (`blast-control.bed-anchor-scale`)

### Optional behavior
- **Per-world overrides** (`world-overrides`) so each world can have its own scaling profile.
- **Crystal self-damage bypass** (`rules.crystal-owner-vanilla-damage`) so the player who triggered/damaged a crystal can still take vanilla crystal damage even when crystal damage is reduced globally.
- **Verbose debug logs** (`telemetry.verbose-debug`) to print before/after values whenever explosion damage is adjusted.

---

## ✅ Compatibility

- API target: **Spigot 1.16.5**
- Declared plugin API version: **1.16**
- Works on Bukkit-family servers including **Spigot / Paper / Folia**
- Java target: **8**

---

## 📦 Installation

1. Build or download the plugin JAR.
2. Place it in your server's `plugins/` folder.
3. Start/restart the server once to generate default config.
4. Edit `plugins/EzExplosionManager/config.yml` to match your balance needs.
5. Run:
   ```
   /ezexplosionmanager reload
   ```
   or restart the server.

---

## ⚙️ Configuration quick guide

Config file path: `plugins/EzExplosionManager/config.yml`

### Multiplier rules
- `1.0` = vanilla damage
- `0.5` = half damage
- `0.0` = no damage
- `>1.0` = increased damage
- invalid/negative values are sanitized to `0.0`

### Core keys

```yaml
telemetry:
  verbose-debug: false

blast-control:
  crystal-scale: 1.0
  tnt-scale: 1.0
  minecart-scale: 1.0
  bed-anchor-scale: 1.0

world-overrides: {}

rules:
  crystal-owner-vanilla-damage: true
```

### Example: safer overworld, dangerous nether

```yaml
blast-control:
  crystal-scale: 0.7
  tnt-scale: 0.6
  minecart-scale: 0.5
  bed-anchor-scale: 1.0

world-overrides:
  world_nether:
    crystal-scale: 1.0
    tnt-scale: 1.0
    minecart-scale: 1.0
    bed-anchor-scale: 1.2

rules:
  crystal-owner-vanilla-damage: true
```

---

## ⌨️ Commands & permissions

### Command
- `/ezexplosionmanager reload`
  - Reloads plugin configuration from disk.

### Alias
- `/eem reload`

### Permission
- `ezexplosionmanager.reload` (default: OP)

---

## 🧠 Notes for admins

- The plugin modifies **player damage values**, not explosion block physics.
- World override names should match world folder names (case-insensitive matching is supported internally).
- Bed/anchor explosions are tracked via recent block-explosion context and proximity detection.
- Crystal owner bypass requires identifying the player that damaged/triggered a crystal before it explodes.

---

## 🧪 Full update regression checklist (run this every plugin update)

> Use this exact order each time you update EzExplosionManager so no feature is missed.

1. **Backup & deploy**
   - Backup old JAR and `config.yml`.
   - Replace JAR with new build.
   - Start server and confirm plugin enables cleanly.

2. **Baseline sanity check**
   - Ensure `config.yml` loads without YAML errors.
   - Run `/ezexplosionmanager reload` and confirm success message.
   - Confirm no stack traces in console.

3. **Command & permission test**
   - As OP/admin: run `/ezexplosionmanager reload` (should pass).
   - As non-permitted user: run `/ezexplosionmanager reload` (should deny).

4. **Global multiplier tests**
   - Set these temporary test values:
     - `crystal-scale: 0.5`
     - `tnt-scale: 0.5`
     - `minecart-scale: 0.5`
     - `bed-anchor-scale: 0.5`
   - Reload config.
   - Trigger each source in same armor/health conditions and verify roughly half vanilla damage.

5. **Zero-damage edge tests**
   - Set each source to `0.0` one at a time.
   - Verify that source deals no player damage.

6. **High-damage edge tests**
   - Set each source to a value like `1.5` or `2.0`.
   - Verify damage increases compared to vanilla baseline.

7. **Invalid value sanitization test**
   - Set a multiplier to a negative number (for example `-1.0`).
   - Reload config and verify behavior matches `0.0` (sanitized).

8. **Crystal owner bypass test**
   - Set `crystal-scale: 0.1` and `crystal-owner-vanilla-damage: true`.
   - Player A damages/places crystal and is caught in own explosion.
   - Confirm Player A receives near-vanilla crystal damage while other players receive scaled damage.
   - Repeat with `crystal-owner-vanilla-damage: false` and confirm everyone receives scaled damage.

9. **Per-world override tests**
   - Configure one world override (example `world_nether`) with different multipliers.
   - Test same explosion source in two worlds:
     - world without override uses global value
     - world with override uses override value

10. **Debug telemetry test**
    - Set `telemetry.verbose-debug: true`.
    - Trigger each explosion source once.
    - Confirm console logs show source and before/after damage values.
    - Set debug back to `false` after verification.

11. **Restart persistence test**
    - Fully restart server.
    - Re-run one sample test per explosion source to confirm stable behavior after reboot.

12. **Final sign-off**
    - Restore intended production config values.
    - Archive test notes + plugin version tested.
    - Mark update as validated.

---

## 🛠️ Build (for developers)

```bash
mvn clean package
```

Output JAR will be in `target/`.

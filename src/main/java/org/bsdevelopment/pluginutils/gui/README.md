# XML‐Driven GUI System

A lightweight, fully‐customizable GUI framework for Spigot 1.21.x that lets you define menus entirely in XML (or generate XML programmatically) and drive them via your plugin code.

---

## Features

- **Pure XML GUI definitions**
    - `<definitions>` for reusable item-templates (with NBT, enchants, flags, attributes, skull-textures, PersistentDataContainer, raw JSON, etc.)
    - `<component slot="…">` for placing items into specific inventory slots
    - Chainable `<action>` tags (`message`, `give`, `close`, `command`, and any you register)

- **Full Spigot API support**
    - Any `Material`, dynamic chest sizes (rows 1–6) or any `InventoryType`
    - Custom display names, lore lines, custom-model-data, skull textures
    - All `ItemFlag`s, `Enchantment`s, and `AttributeModifier`s
    - Persistent-data for all common types: `STRING`, `BOOLEAN`, `INT`, `BYTE`, `DOUBLE`, `FLOAT`, `LONG`, `SHORT`
    - Raw NBT JSON fallback via [NBTJSON / CompoundData]

- **Java API to generate XML**
    - Build menus in code via `XmlGuiOutput`, then `write()` your `example-gui.xml`
    - Keeps a one‐to‐one mapping so your input loader (`XmlGuiInput`) and output (`XmlGuiOutput`) stay in sync

---

## Quick Start

### 1. Add Loader to Your Plugin

```java
public class MyPlugin extends JavaPlugin {
  private CustomGui gui;

  @Override
  public void onEnable() {
    // load from data folder / example.xml
    File file = new File(getDataFolder(), "example-gui.xml");
    saveResource("example-gui.xml", false);
    gui = XmlGuiInput.load(this, file);

    getCommand("openmenu").setExecutor((sender, cmd, label, args) -> {
      if (sender instanceof Player p) gui.open(p);
      return true;
    });
  }
}
```

### 2. XML Schema Overview

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gui title="&eMy Menu"           <!-- &-color codes allowed even HEX colors via &#FFFFFF -->
     inventory-type="CHEST"      <!-- or use rows="3"  inventory-type has priority over rows -->
     rows="3">

  <!-- 1) Reusable item templates -->
  <definitions>
    <item-definition
      id="poweredSword"            <!-- unique id -->
      material="DIAMOND_SWORD"
      amount="1"
      name="&cPowered Sword"
      unbreakable="true"
      skull-texture="http://textures.minecraft.net/texture/..." >

      <lore>
        <line>Electrified</line>
        <line>Handle with care</line>
      </lore>

      <flags>
        <flag>HIDE_ENCHANTS</flag>
        <flag>HIDE_ATTRIBUTES</flag>
      </flags>

      <enchants>
        <enchant type="minecraft:sharpness" level="5"/>
        <enchant type="minecraft:unbreaking" level="3"/>
      </enchants>

      <attributes>
        <attribute
          attribute="minecraft:generic_attack_damage"
          amount="8.0"
          operation="ADD_NUMBER"
          slot="HAND"/>
      </attributes>

      <persistent-data-list>
        <persistent-data key="owner"      type="STRING">PlayerName</persistent-data>
        <persistent-data key="isActive"   type="BOOLEAN">true</persistent-data>
        <persistent-data key="powerLevel" type="INT">42</persistent-data>
        <persistent-data key="ratio"      type="DOUBLE">0.75</persistent-data>
        <persistent-data key="speed"      type="FLOAT">1.5</persistent-data>
        <persistent-data key="bigNum"     type="LONG">1234567890123</persistent-data>
        <persistent-data key="shortVal"   type="SHORT">123</persistent-data>
        <persistent-data key="smallByte"  type="BYTE">7</persistent-data>
      </persistent-data-list>

      <nbt-json>
        {"display":{"Name":"{"text":"NBT Sword"}"}, "CustomModelData":99}
      </nbt-json>
    </item-definition>
  </definitions>

  <!-- 2) Place items into slots by zero-based slot index -->
  <component type="item" slot="10" item-id="poweredSword">
    <action type="message">&aYou picked up the sword!</action>
    <action type="give"    item-id="poweredSword" amount="1"/>
    <action type="close"/>
  </component>
</gui>
```

### 3. Register New Actions

```java
ActionRegistry.register("heal", elm -> event -> {
  event.setCancelled(true);
  if (event.getWhoClicked() instanceof Player player) {
    player.setHealth(player.getMaxHealth());
    player.sendMessage("You feel rejuvenated!");
  }
});
```

### 4. Generating XML in Code

```java
XmlGuiOutput out = new XmlGuiOutput("&eMy GUI", 3);                 // 3 rows
out.addDefinition("gem", ItemBuilder.of(Material.RABBIT_FOOT).withName("&aSpeed Gem").withLore("Quick","Light"));

var comp = new XmlGuiOutput.Component(13, "gem");
comp.addAction(new XmlGuiOutput.Action("give", Map.of("item-id","gem","amount","3"), ""));
out.addComponent(comp);

out.write(new File(getDataFolder(), "generated-gui.xml"));
```

---

## Supported Tags & Attributes

| Tag                                            | Description                                                                                |
|------------------------------------------------|--------------------------------------------------------------------------------------------|
| `<gui>`                                        | Root. Attributes: `title`, _either_ `inventory-type` _or_ `rows`.                          |
| `<definitions>`                                | Container for `<item-definition>`                                                          |
| `<item-definition>`                            | Template: `id`, `material`, `amount`, `name`, `unbreakable`, `skull-texture`               |
| `<lore>` / `<line>`                            | Multi-line lore                                                                            |
| `<flags>` / `<flag>`                           | Any `ItemFlag` (e.g. `HIDE_ENCHANTS`)                                                      |
| `<enchants>` / `<enchant>`                     | Enchantment key & level                                                                    |
| `<attributes>` / `<attribute>`                 | Attribute key, amount, operation, slot-group `HAND`, `ARMOR`, etc.                         |
| `<persistent-data-list>` / `<persistent-data>` | Key, type (`STRING`, `BOOLEAN`, `INT`, `BYTE`, `DOUBLE`, `FLOAT`, `LONG`, `SHORT`), value  |
| `<nbt-json>`                                   | Full raw NBT via JSON ((CompoundData) )                                                    |
| `<component>`                                  | Place item: `type="item"`, `slot`, _either_ `item-id` _or_ inline attrs                    |
| `<action>`                                     | Click action: `type`, any additional attributes (`item-id`, `amount`, etc.), optional text |

---

## Extending & Troubleshooting

- All XML parsing errors throw `XmlValidationException` with clear `[tag@attr=val] Error… Hint: …`.
- To add new tags (e.g. custom potion data), update `XmlGuiInput.parseItemBuilder()` and mirror in `XmlGuiOutput.buildDefinitionElement()`.
- Ensure your plugin’s `data/…-gui.xml` is UTF-8 encoded and placed in your JAR `resources` if you use `saveResource()`.


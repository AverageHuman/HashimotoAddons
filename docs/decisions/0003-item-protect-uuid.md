# 0003: Content-Keyed Item Protection Is a Shared Protective Feature

Status: Accepted

## Context

HashimotoAddons needs an item protection feature that identifies the protected item instance rather than the item name, so same-name items do not share protection.
Some items do not retain a custom hidden stack tag reliably enough for protection to depend on it alone.

## Decision

Store protected item identifiers as either hidden legacy NBT IDs or content-based protection keys derived from the item's item ID, damage value, and user-visible NBT. Expose protection through the `/protectitem` command in both Safe and Full builds.

When the player is holding the trigger item used for the interaction guard, the mod blocks clicks on protected items and blocks dropping protected items. The trigger item may be identified by display name first and may fall back to TNT when name matching is not reliable.

## Consequences

- Protection applies to the specific item stack instance when a legacy hidden ID is present. For items without a reliable hidden ID, protection falls back to a content key, which can group otherwise identical stacks.
- Item protection remains separate from slot-based item lock.
- The feature is available in both supported variants because it is protective quality-of-life rather than gameplay automation.

use crate::error::AppError;
use crate::ecosystem;
use crate::instances;
use crate::paths;
use crate::settings;
use serde_json::json;
use std::fs;

/// Writes `{game}/config/primeclient/profiles/default.json` for the mod.
pub fn sync_instance(instance_id: &str) -> Result<(), AppError> {
    let game = paths::instance_game_dir(instance_id);
    let profile = game
        .join("config")
        .join("primeclient")
        .join("profiles")
        .join("default.json");
    if let Some(parent) = profile.parent() {
        fs::create_dir_all(parent)?;
    }

    let equipped = ecosystem::equipped_for_bridge()?;
    let mut cape = "cape-prime";
    let mut wings = "wings-light";
    let mut badge = "badge-founder";
    for id in &equipped {
        match id.as_str() {
            "cape-prime" => cape = "cape-prime",
            "wings-ember" => wings = "wings-light",
            "badge-founder" | "badge-veteran" => badge = "badge-founder",
            _ => {}
        }
    }
    let discord = settings::load()?.discord_rpc;

    let mut existing = if profile.exists() {
        serde_json::from_str(&fs::read_to_string(&profile)?).unwrap_or(json!({}))
    } else {
        json!({})
    };
    if let Some(obj) = existing.as_object_mut() {
        obj.insert(
            "cosmetics".into(),
            json!({ "CAPE": cape, "WINGS": wings, "BADGE": badge }),
        );
        obj.insert(
            "modules".into(),
            json!({ "discord-rpc": { "enabled": discord } }),
        );
    }
    fs::write(profile, serde_json::to_string_pretty(&existing)?)?;
    Ok(())
}

pub fn sync_all_prime_instances() -> Result<(), AppError> {
    let list = instances::list()?;
    for inst in list {
        let include = inst
            .get("includePrimeMod")
            .and_then(|v| v.as_bool())
            .unwrap_or(false);
        if !include {
            continue;
        }
        if let Some(id) = inst.get("id").and_then(|v| v.as_str()) {
            sync_instance(id)?;
        }
    }
    Ok(())
}

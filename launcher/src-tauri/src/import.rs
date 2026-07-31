//! Import instances from Prism / MultiMC / Lunar / Feather / Dawn / Modrinth App.
use crate::error::AppError;
use crate::instances;
use crate::paths;
use crate::settings;
use serde_json::{json, Value};
use sha1::{Digest, Sha1};
use std::fs;
use std::path::{Path, PathBuf};

const COPY_DIRS: &[&str] = &["mods", "config", "resourcepacks", "shaderpacks", "screenshots"];
const COPY_FILES: &[&str] = &["options.txt"];

#[derive(Clone)]
struct ParsedInstance {
    name: String,
    minecraft_version: String,
    loader: String,
    fabric_loader_version: Option<String>,
    ram_mb: Option<u32>,
    game_dir: PathBuf,
}

fn app_data() -> PathBuf {
    dirs::data_dir().unwrap_or_else(|| PathBuf::from("."))
}

fn local_app_data() -> PathBuf {
    std::env::var_os("LOCALAPPDATA")
        .map(PathBuf::from)
        .or_else(|| dirs::data_local_dir())
        .unwrap_or_else(|| PathBuf::from("."))
}

fn user_profile() -> PathBuf {
    dirs::home_dir().unwrap_or_else(|| PathBuf::from("."))
}

fn exists(path: &Path) -> bool {
    path.exists()
}

fn is_dir(path: &Path) -> bool {
    path.is_dir()
}

fn make_id(source: &str, root: &str, name: &str) -> String {
    let mut hasher = Sha1::new();
    hasher.update(format!("{source}|{root}|{name}").as_bytes());
    hex::encode(hasher.finalize())[..16].to_string()
}

fn parse_ini(raw: &str) -> std::collections::HashMap<String, String> {
    let mut out = std::collections::HashMap::new();
    for line in raw.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('#') || trimmed.starts_with('[') {
            continue;
        }
        if let Some((k, v)) = trimmed.split_once('=') {
            out.insert(k.trim().to_string(), v.trim().to_string());
        }
    }
    out
}

fn resolve_minecraft_game_dir(instance_dir: &Path) -> PathBuf {
    let candidates = [
        instance_dir.join(".minecraft"),
        instance_dir.join("minecraft"),
        instance_dir.to_path_buf(),
    ];
    for dir in &candidates {
        if !is_dir(dir) {
            continue;
        }
        if exists(&dir.join("mods"))
            || exists(&dir.join("options.txt"))
            || exists(&dir.join("saves"))
            || exists(&dir.join("resourcepacks"))
        {
            return dir.clone();
        }
    }
    if is_dir(&instance_dir.join(".minecraft")) {
        instance_dir.join(".minecraft")
    } else {
        instance_dir.to_path_buf()
    }
}

fn folder_flags(game_dir: &Path) -> (bool, bool, bool, bool) {
    (
        exists(&game_dir.join("mods")),
        exists(&game_dir.join("resourcepacks")),
        exists(&game_dir.join("screenshots")),
        exists(&game_dir.join("options.txt")),
    )
}

fn parse_mmc_pack(instance_dir: &Path) -> Option<(String, String, Option<String>)> {
    let raw = fs::read_to_string(instance_dir.join("mmc-pack.json")).ok()?;
    let pack: Value = serde_json::from_str(&raw).ok()?;
    let components = pack.get("components")?.as_array()?;
    let mc = components.iter().find(|c| c.get("uid").and_then(|v| v.as_str()) == Some("net.minecraft"));
    let fabric = components.iter().find(|c| {
        let uid = c.get("uid").and_then(|v| v.as_str()).unwrap_or("");
        let name = c
            .get("cachedName")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_lowercase();
        uid == "net.fabricmc.fabric-loader"
            || uid == "net.fabricmc.fabric-loader-intermediary"
            || name.contains("fabric loader")
    });
    let version = mc?.get("version")?.as_str()?.trim().to_string();
    if version.is_empty() {
        return None;
    }
    let fabric_ver = fabric
        .and_then(|f| f.get("version").and_then(|v| v.as_str()).map(str::to_string));
    let loader = if fabric_ver.is_some() {
        "fabric"
    } else {
        "vanilla"
    };
    Some((version, loader.into(), fabric_ver))
}

fn parse_prism_like(instance_dir: &Path) -> Option<ParsedInstance> {
    let cfg_path = instance_dir.join("instance.cfg");
    if !exists(&cfg_path) && !exists(&instance_dir.join("mmc-pack.json")) {
        return None;
    }
    let mut name = instance_dir
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("Instance")
        .to_string();
    let mut intended_version = String::new();
    let mut ram_mb = None;
    if exists(&cfg_path) {
        if let Ok(raw) = fs::read_to_string(&cfg_path) {
            let cfg = parse_ini(&raw);
            name = cfg
                .get("name")
                .or(cfg.get("InstanceName"))
                .cloned()
                .unwrap_or(name);
            intended_version = cfg
                .get("IntendedVersion")
                .or(cfg.get("MinecraftVersion"))
                .cloned()
                .unwrap_or_default();
            if let Some(max) = cfg
                .get("MaxMemAlloc")
                .or(cfg.get("MaxMemory"))
                .and_then(|v| v.parse::<u32>().ok())
            {
                if max >= 512 {
                    ram_mb = Some(max);
                }
            }
        }
    }
    let pack = parse_mmc_pack(instance_dir);
    let minecraft_version = pack
        .as_ref()
        .map(|(v, ..)| v.clone())
        .filter(|v| !v.is_empty())
        .unwrap_or_else(|| {
            if intended_version.is_empty() {
                "1.21.11".into()
            } else {
                intended_version
            }
        });
    let loader = pack
        .as_ref()
        .map(|(_, l, _)| l.clone())
        .unwrap_or_else(|| "vanilla".into());
    let fabric_loader_version = pack.and_then(|(_, _, f)| f);
    let game_dir = resolve_minecraft_game_dir(instance_dir);
    Some(ParsedInstance {
        name: name.chars().take(32).collect(),
        minecraft_version,
        loader,
        fabric_loader_version,
        ram_mb,
        game_dir,
    })
}

fn list_prism_like(root: &Path) -> Vec<ParsedInstance> {
    if !is_dir(root) {
        return vec![];
    }
    let Ok(entries) = fs::read_dir(root) else {
        return vec![];
    };
    let mut out = vec![];
    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().to_string();
        if name.starts_with('.') || !entry.path().is_dir() {
            continue;
        }
        if let Some(parsed) = parse_prism_like(&entry.path()) {
            out.push(parsed);
        }
    }
    out
}

fn first_existing(paths: &[PathBuf]) -> Option<PathBuf> {
    paths.iter().find(|p| is_dir(p)).cloned()
}

fn launcher_roots(source: &str) -> Vec<PathBuf> {
    match source {
        "prism" => vec![
            app_data().join("PrismLauncher").join("instances"),
            local_app_data().join("PrismLauncher").join("instances"),
            user_profile().join("PrismLauncher").join("instances"),
        ],
        "multimc" => vec![
            app_data().join("MultiMC").join("instances"),
            app_data().join("multimc").join("instances"),
            local_app_data().join("MultiMC").join("instances"),
            user_profile().join("MultiMC").join("instances"),
        ],
        "lunar" => vec![
            user_profile().join(".lunarclient"),
            app_data().join("lunarclient"),
            local_app_data().join("lunarclient"),
        ],
        "feather" => vec![
            app_data().join("Feather").join("instances"),
            app_data().join("Feather Launcher").join("instances"),
            app_data().join("feather").join("instances"),
            local_app_data().join("Feather").join("instances"),
            user_profile().join(".feather").join("instances"),
        ],
        "dawn" => vec![
            app_data().join("DawnClient").join("instances"),
            app_data().join("Dawn").join("instances"),
            app_data().join("dawnclient").join("instances"),
            local_app_data().join("DawnClient").join("instances"),
            user_profile().join(".dawnclient").join("instances"),
        ],
        "modrinth" => vec![
            app_data().join("ModrinthApp").join("profiles"),
            app_data().join("modrinth").join("profiles"),
            local_app_data().join("ModrinthApp").join("profiles"),
            user_profile()
                .join(".local")
                .join("share")
                .join("modrinthapp")
                .join("profiles"),
        ],
        _ => vec![],
    }
}

fn launcher_label(source: &str) -> &'static str {
    match source {
        "prism" => "Prism Launcher",
        "multimc" => "MultiMC",
        "lunar" => "Lunar Client",
        "feather" => "Feather",
        "dawn" => "Dawn Client",
        "modrinth" => "Modrinth App",
        _ => "Unknown",
    }
}

fn list_lunar(root: &Path) -> Vec<ParsedInstance> {
    let mut out = vec![];
    let offline_roots = [
        root.join("offline").join("multiver"),
        root.join("offline"),
        root.join("game-cache"),
    ];
    for offline in &offline_roots {
        if !is_dir(offline) {
            continue;
        }
        let Ok(entries) = fs::read_dir(offline) else {
            continue;
        };
        for entry in entries.flatten() {
            if !entry.path().is_dir() {
                continue;
            }
            let name = entry.file_name().to_string_lossy().to_string();
            let version_guess = name.trim_start_matches(['v', 'V']).to_string();
            let looks_like_version = version_guess
                .chars()
                .next()
                .map(|c| c.is_ascii_digit())
                .unwrap_or(false)
                && version_guess.contains('.');
            let dir = entry.path();
            let game_dir = if exists(&dir.join("options.txt")) || exists(&dir.join("mods")) {
                dir.clone()
            } else {
                resolve_minecraft_game_dir(&dir)
            };
            if !looks_like_version
                && !exists(&game_dir.join("options.txt"))
                && !exists(&game_dir.join("mods"))
            {
                continue;
            }
            let display = format!(
                "Lunar {}",
                if looks_like_version {
                    &version_guess
                } else {
                    &name
                }
            );
            out.push(ParsedInstance {
                name: display.chars().take(32).collect(),
                minecraft_version: if looks_like_version {
                    version_guess
                } else {
                    "1.21.11".into()
                },
                loader: "vanilla".into(),
                fabric_loader_version: None,
                ram_mb: None,
                game_dir,
            });
        }
    }
    if out.is_empty() {
        let shared = root.join("settings").join("game");
        if is_dir(&shared) {
            out.push(ParsedInstance {
                name: "Lunar Shared".into(),
                minecraft_version: "1.21.11".into(),
                loader: "vanilla".into(),
                fabric_loader_version: None,
                ram_mb: None,
                game_dir: shared,
            });
        }
    }
    let mut seen = std::collections::HashSet::new();
    out.retain(|item| seen.insert(item.game_dir.to_string_lossy().to_string()));
    out
}

fn list_modrinth(root: &Path) -> Vec<ParsedInstance> {
    if !is_dir(root) {
        return vec![];
    }
    let Ok(entries) = fs::read_dir(root) else {
        return vec![];
    };
    let mut out = vec![];
    for entry in entries.flatten() {
        let name_raw = entry.file_name().to_string_lossy().to_string();
        if name_raw.starts_with('.') || !entry.path().is_dir() {
            continue;
        }
        let profile_dir = entry.path();
        let mut name = name_raw;
        let mut minecraft_version = "1.21.11".to_string();
        let mut loader = "fabric".to_string();
        let meta_path = profile_dir.join("profile.json");
        if exists(&meta_path) {
            if let Ok(raw) = fs::read_to_string(&meta_path) {
                if let Ok(meta) = serde_json::from_str::<Value>(&raw) {
                    if let Some(n) = meta.get("name").and_then(|v| v.as_str()) {
                        name = n.to_string();
                    }
                    if let Some(v) = meta.get("game_version").and_then(|v| v.as_str()) {
                        minecraft_version = v.to_string();
                    }
                    loader = if meta.get("loader").and_then(|v| v.as_str()) == Some("fabric") {
                        "fabric".into()
                    } else {
                        "vanilla".into()
                    };
                }
            }
        }
        out.push(ParsedInstance {
            name: name.chars().take(32).collect(),
            minecraft_version,
            loader,
            fabric_loader_version: None,
            ram_mb: None,
            game_dir: profile_dir,
        });
    }
    out
}

fn list_source(source: &str, root: &Path) -> Vec<ParsedInstance> {
    match source {
        "lunar" => list_lunar(root),
        "modrinth" => list_modrinth(root),
        _ => list_prism_like(root),
    }
}

fn to_dto(source: &str, root: &str, parsed: &ParsedInstance) -> Value {
    let (has_mods, has_rp, has_ss, has_opts) = folder_flags(&parsed.game_dir);
    let id = make_id(
        source,
        root,
        &format!("{}|{}", parsed.name, parsed.game_dir.display()),
    );
    json!({
        "id": id,
        "source": source,
        "name": parsed.name,
        "minecraftVersion": parsed.minecraft_version,
        "loader": parsed.loader,
        "fabricLoaderVersion": parsed.fabric_loader_version,
        "gameDir": parsed.game_dir.to_string_lossy(),
        "ramMb": parsed.ram_mb,
        "hasMods": has_mods,
        "hasResourcePacks": has_rp,
        "hasScreenshots": has_ss,
        "hasOptions": has_opts
    })
}

fn copy_dir_recursive(from: &Path, to: &Path) -> Result<(), AppError> {
    fs::create_dir_all(to)?;
    for entry in fs::read_dir(from)? {
        let entry = entry?;
        let dest = to.join(entry.file_name());
        if entry.path().is_dir() {
            copy_dir_recursive(&entry.path(), &dest)?;
        } else {
            fs::copy(entry.path(), dest)?;
        }
    }
    Ok(())
}

fn copy_content(source_game: &Path, dest_game: &Path) -> Result<(), AppError> {
    fs::create_dir_all(dest_game)?;
    for dir in COPY_DIRS {
        let from = source_game.join(dir);
        if is_dir(&from) {
            copy_dir_recursive(&from, &dest_game.join(dir))?;
        }
    }
    for file in COPY_FILES {
        let from = source_game.join(file);
        if exists(&from) {
            fs::copy(&from, dest_game.join(file))?;
        }
    }
    Ok(())
}

const SOURCES: &[&str] = &["prism", "multimc", "lunar", "feather", "dawn", "modrinth"];

pub fn detect() -> Result<Vec<Value>, AppError> {
    let mut results = vec![];
    for id in SOURCES {
        let roots = launcher_roots(id);
        let Some(root) = first_existing(&roots) else {
            results.push(json!({
                "id": id,
                "label": launcher_label(id),
                "rootPath": roots.first().map(|p| p.to_string_lossy().to_string()).unwrap_or_default(),
                "instanceCount": 0,
                "available": false
            }));
            continue;
        };
        let instances = list_source(id, &root);
        results.push(json!({
            "id": id,
            "label": launcher_label(id),
            "rootPath": root.to_string_lossy(),
            "instanceCount": instances.len(),
            "available": !instances.is_empty()
        }));
    }
    Ok(results)
}

pub fn list(source: String) -> Result<Vec<Value>, AppError> {
    let roots = launcher_roots(&source);
    let Some(root) = first_existing(&roots) else {
        return Ok(vec![]);
    };
    let root_str = root.to_string_lossy().to_string();
    let parsed = list_source(&source, &root);
    Ok(parsed
        .iter()
        .map(|p| to_dto(&source, &root_str, p))
        .collect())
}

fn import_one(instance_id: &str, source: &str) -> Value {
    let Ok(candidates) = list(source.to_string()) else {
        return json!({ "ok": false, "error": "Failed to list instances." });
    };
    let Some(found) = candidates
        .iter()
        .find(|c| c.get("id").and_then(|v| v.as_str()) == Some(instance_id))
    else {
        return json!({ "ok": false, "error": "Instance not found for import." });
    };
    let settings = match settings::load() {
        Ok(s) => s,
        Err(e) => return json!({ "ok": false, "error": e.to_string() }),
    };
    let ram = found
        .get("ramMb")
        .and_then(|v| v.as_u64())
        .unwrap_or(settings.default_ram_mb as u64);
    let create_input = json!({
        "name": found.get("name").and_then(|v| v.as_str()).unwrap_or("Imported"),
        "minecraftVersion": found.get("minecraftVersion").and_then(|v| v.as_str()).unwrap_or("1.21.11"),
        "loader": found.get("loader").and_then(|v| v.as_str()).unwrap_or("vanilla"),
        "fabricLoaderVersion": found.get("fabricLoaderVersion"),
        "includePrimeMod": false,
        "ramMb": ram
    });
    let created = match instances::create(create_input) {
        Ok(v) => v,
        Err(e) => return json!({ "ok": false, "error": e.to_string() }),
    };
    if created.get("ok").and_then(|v| v.as_bool()) != Some(true) {
        return json!({
            "ok": false,
            "error": created.get("error").and_then(|v| v.as_str()).unwrap_or("Failed to create instance.")
        });
    }
    let new_id = created
        .get("id")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let source_dir = found
        .get("gameDir")
        .and_then(|v| v.as_str())
        .map(PathBuf::from)
        .unwrap_or_default();
    if let Err(e) = copy_content(&source_dir, &paths::instance_game_dir(&new_id)) {
        return json!({ "ok": false, "error": e.to_string(), "instanceId": new_id });
    }
    json!({ "ok": true, "instanceId": new_id })
}

pub fn import_run(source: String, instance_ids: Vec<String>) -> Result<Value, AppError> {
    if instance_ids.is_empty() {
        return Ok(json!({ "ok": false, "imported": [], "error": "No instances selected." }));
    }
    let mut imported = vec![];
    for id in &instance_ids {
        imported.push(import_one(id, &source));
    }
    let any_ok = imported
        .iter()
        .any(|r| r.get("ok").and_then(|v| v.as_bool()) == Some(true));
    Ok(json!({
        "ok": any_ok,
        "imported": imported,
        "error": if any_ok { Value::Null } else {
            imported.first().and_then(|r| r.get("error").cloned()).unwrap_or(json!("Import failed."))
        }
    }))
}

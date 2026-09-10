//! Local Minecraft skin library (parity with SkinLibraryService).
use crate::error::AppError;
use crate::paths;
use crate::settings;
use base64::{engine::general_purpose::STANDARD, Engine};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::fs;
use std::path::{Path, PathBuf};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LocalSkin {
    pub id: String,
    pub name: String,
    pub file_name: String,
    pub data_url: String,
    pub created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct SkinManifest {
    version: u32,
    skins: Vec<SkinEntry>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct SkinEntry {
    id: String,
    name: String,
    file_name: String,
    created_at: String,
}

fn skins_dir() -> PathBuf {
    paths::user_data_dir().join("skins")
}

fn manifest_path() -> PathBuf {
    skins_dir().join("manifest.json")
}

fn ensure_dir() -> Result<(), AppError> {
    fs::create_dir_all(skins_dir())?;
    Ok(())
}

fn load_manifest() -> Result<SkinManifest, AppError> {
    ensure_dir()?;
    let path = manifest_path();
    if !path.exists() {
        return Ok(SkinManifest {
            version: 1,
            skins: vec![],
        });
    }
    let raw = fs::read_to_string(path)?;
    Ok(serde_json::from_str(&raw).unwrap_or(SkinManifest {
        version: 1,
        skins: vec![],
    }))
}

fn save_manifest(manifest: &SkinManifest) -> Result<(), AppError> {
    ensure_dir()?;
    fs::write(manifest_path(), serde_json::to_string_pretty(manifest)?)?;
    Ok(())
}

fn to_data_url(file_path: &Path) -> Result<String, AppError> {
    let buf = fs::read(file_path)?;
    Ok(format!("data:image/png;base64,{}", STANDARD.encode(buf)))
}

pub fn list() -> Result<Vec<LocalSkin>, AppError> {
    let manifest = load_manifest()?;
    let mut out = Vec::new();
    for entry in manifest.skins {
        let path = skins_dir().join(&entry.file_name);
        match to_data_url(&path) {
            Ok(data_url) => out.push(LocalSkin {
                id: entry.id,
                name: entry.name,
                file_name: entry.file_name,
                data_url,
                created_at: entry.created_at,
            }),
            Err(_) => continue,
        }
    }
    Ok(out)
}

pub fn import_png(source: &Path) -> Result<Value, AppError> {
    if !source.exists() {
        return Ok(json!({ "ok": false, "error": "File not found." }));
    }
    let id = Uuid::new_v4().to_string();
    let file_name = format!("{id}.png");
    ensure_dir()?;
    fs::copy(source, skins_dir().join(&file_name))?;

    let name = source
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Custom Skin")
        .chars()
        .take(32)
        .collect::<String>();
    let name = if name.is_empty() {
        "Custom Skin".into()
    } else {
        name
    };
    let created_at = chrono::Utc::now().to_rfc3339();
    let mut manifest = load_manifest()?;
    manifest.skins.insert(
        0,
        SkinEntry {
            id: id.clone(),
            name: name.clone(),
            file_name: file_name.clone(),
            created_at: created_at.clone(),
        },
    );
    save_manifest(&manifest)?;
    let data_url = to_data_url(&skins_dir().join(&file_name))?;
    Ok(json!({
        "ok": true,
        "skin": {
            "id": id,
            "name": name,
            "fileName": file_name,
            "dataUrl": data_url,
            "createdAt": created_at
        }
    }))
}

pub fn remove(id: String) -> Result<Value, AppError> {
    let mut manifest = load_manifest()?;
    let Some(entry) = manifest.skins.iter().find(|s| s.id == id).cloned() else {
        return Ok(json!({ "ok": false, "error": "Skin not found." }));
    };
    manifest.skins.retain(|s| s.id != id);
    save_manifest(&manifest)?;
    let _ = fs::remove_file(skins_dir().join(&entry.file_name));
    let mut s = settings::load()?;
    if s.active_skin_id.as_deref() == Some(&id) {
        s.active_skin_id = None;
        settings::save(&s)?;
    }
    Ok(json!({ "ok": true }))
}

pub fn set_active(id: Option<String>) -> Result<Value, AppError> {
    if let Some(ref skin_id) = id {
        let manifest = load_manifest()?;
        if !manifest.skins.iter().any(|s| &s.id == skin_id) {
            return Ok(json!({ "ok": false }));
        }
    }
    let mut s = settings::load()?;
    s.active_skin_id = id;
    settings::save(&s)?;
    Ok(json!({ "ok": true }))
}

pub fn active_data_url() -> Result<Option<String>, AppError> {
    let s = settings::load()?;
    let Some(id) = s.active_skin_id else {
        return Ok(None);
    };
    Ok(list()?.into_iter().find(|s| s.id == id).map(|s| s.data_url))
}

/// Absolute path to the active skin PNG for bridge sync, if any.
pub fn active_file_path() -> Result<Option<PathBuf>, AppError> {
    let s = settings::load()?;
    let Some(id) = s.active_skin_id else {
        return Ok(None);
    };
    let manifest = load_manifest()?;
    let Some(entry) = manifest.skins.iter().find(|e| e.id == id) else {
        return Ok(None);
    };
    let path = skins_dir().join(&entry.file_name);
    if path.exists() {
        Ok(Some(path))
    } else {
        Ok(None)
    }
}

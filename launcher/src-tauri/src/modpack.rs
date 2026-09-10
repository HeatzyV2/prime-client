//! Installs Fabric API + Prime Client jar before launch (same behaviour as Electron ModPackService).
use crate::error::AppError;
use crate::instances;
use crate::logs;
use crate::minecraft_targets::{
    is_any_prime_jar, is_prime_jar_for_prefix, resolve_target, MinecraftTarget,
};
use crate::paths;
use serde_json::{json, Value};
use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};
use std::time::Duration;
use tauri::{AppHandle, Emitter};

const FABRIC_API_PROJECT: &str = "P7dR8mSH";
const OWNER: &str = "HeatzyV2";
const REPO: &str = "prime-client";
const API_TIMEOUT: Duration = Duration::from_secs(30);
const DOWNLOAD_TIMEOUT: Duration = Duration::from_secs(120);

fn api_client() -> Result<reqwest::Client, AppError> {
    reqwest::Client::builder()
        .timeout(API_TIMEOUT)
        .connect_timeout(Duration::from_secs(15))
        .user_agent("Prime-Launcher/2.3.2")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))
}

fn download_client() -> Result<reqwest::Client, AppError> {
    reqwest::Client::builder()
        .timeout(DOWNLOAD_TIMEOUT)
        .connect_timeout(Duration::from_secs(15))
        .user_agent("Prime-Launcher/2.3.2")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))
}

fn emit_progress(app: Option<&AppHandle>, detail: &str, percent: u32) {
    logs::append("info", detail, Some("mods"));
    if let Some(app) = app {
        let _ = app.emit(
            "launch:progress",
            json!({ "phase": "mods", "detail": detail, "percent": percent }),
        );
    }
}

async fn download(url: &str, dest: &Path) -> Result<(), AppError> {
    let res = download_client()?
        .get(url)
        .send()
        .await
        .map_err(|e| {
            AppError::Message(format!(
                "Download failed ({url}): {e}. Check your network or try again."
            ))
        })?;
    if !res.status().is_success() {
        return Err(AppError::Message(format!(
            "Download failed HTTP {} for {url}",
            res.status()
        )));
    }
    let bytes = res.bytes().await.map_err(|e| {
        AppError::Message(format!("Download interrupted ({url}): {e}"))
    })?;
    if let Some(parent) = dest.parent() {
        fs::create_dir_all(parent)?;
    }
    let tmp = dest.with_extension("jar.part");
    {
        let mut f = fs::File::create(&tmp)?;
        f.write_all(&bytes)?;
    }
    if dest.exists() {
        let _ = fs::remove_file(dest);
    }
    fs::rename(&tmp, dest).or_else(|_| {
        fs::copy(&tmp, dest)?;
        fs::remove_file(&tmp)?;
        Ok::<(), AppError>(())
    })?;
    Ok(())
}

fn fabric_api_already_present(mods: &Path, preferred: Option<&str>) -> bool {
    if let Some(p) = preferred {
        let dest = mods.join(format!("fabric-api-{p}.jar"));
        if dest.exists() {
            if let Ok(meta) = fs::metadata(&dest) {
                if meta.len() > 0 {
                    return true;
                }
            }
        }
    }
    // Any non-empty fabric-api jar is enough to launch (avoid re-download hang).
    let Ok(rd) = fs::read_dir(mods) else {
        return false;
    };
    rd.flatten().any(|e| {
        let name = e.file_name().to_string_lossy().to_string();
        name.starts_with("fabric-api-")
            && name.ends_with(".jar")
            && fs::metadata(e.path()).map(|m| m.len() > 0).unwrap_or(false)
    })
}

async fn ensure_fabric_api(
    instance_id: &str,
    mc_version: &str,
    preferred: Option<&str>,
    app: Option<&AppHandle>,
) -> Result<(), AppError> {
    let mods = paths::instance_mods_dir(instance_id);
    fs::create_dir_all(&mods)?;
    if fabric_api_already_present(&mods, preferred) {
        emit_progress(app, "Fabric API already installed.", 18);
        return Ok(());
    }

    emit_progress(app, "Downloading Fabric API from Modrinth…", 16);

    let query = format!(
        "game_versions={}&loaders={}",
        urlencoding::encode(&format!("[\"{mc_version}\"]")),
        urlencoding::encode("[\"fabric\"]")
    );
    let url = format!("https://api.modrinth.com/v2/project/{FABRIC_API_PROJECT}/version?{query}");
    let res = api_client()?
        .get(&url)
        .send()
        .await
        .map_err(|e| {
            AppError::Message(format!(
                "Modrinth unreachable while fetching Fabric API: {e}"
            ))
        })?;
    if !res.status().is_success() {
        return Err(AppError::Message(format!(
            "Modrinth API error ({}) while fetching Fabric API for Minecraft {mc_version}.",
            res.status()
        )));
    }
    let versions: Vec<Value> = res
        .json()
        .await
        .map_err(|e| AppError::Message(format!("Invalid Modrinth response: {e}")))?;

    let chosen = preferred
        .and_then(|p| {
            versions
                .iter()
                .find(|v| v.get("version_number").and_then(|x| x.as_str()) == Some(p))
        })
        .or_else(|| versions.first())
        .ok_or_else(|| AppError::Message(format!("No Fabric API for Minecraft {mc_version}")))?;

    let file = chosen
        .get("files")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter()
                .find(|f| f.get("primary").and_then(|p| p.as_bool()).unwrap_or(false))
                .or_else(|| arr.first())
        })
        .ok_or_else(|| AppError::Message("Fabric API has no file".into()))?;
    let dl = file
        .get("url")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Fabric API download url missing".into()))?;
    let filename = file
        .get("filename")
        .and_then(|v| v.as_str())
        .unwrap_or("fabric-api.jar");
    let dest = mods.join(filename);
    if !dest.exists() || fs::metadata(&dest).map(|m| m.len() == 0).unwrap_or(true) {
        download(dl, &dest).await?;
    }
    emit_progress(app, "Fabric API ready.", 22);
    Ok(())
}

fn find_local_build(target: &MinecraftTarget) -> Option<PathBuf> {
    let libs = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .join(target.local_build_dir)
        .join("build")
        .join("libs");
    let Ok(rd) = fs::read_dir(libs) else {
        return None;
    };
    let mut best: Option<PathBuf> = None;
    for e in rd.flatten() {
        let name = e.file_name().to_string_lossy().to_string();
        if is_prime_jar_for_prefix(&name, target.jar_prefix) {
            best = Some(e.path());
        }
    }
    best
}

fn find_installed(mods: &Path, prefix: &str) -> Option<PathBuf> {
    let Ok(rd) = fs::read_dir(mods) else {
        return None;
    };
    let mut best: Option<PathBuf> = None;
    for e in rd.flatten() {
        let name = e.file_name().to_string_lossy().to_string();
        if is_prime_jar_for_prefix(&name, prefix) {
            if fs::metadata(e.path()).map(|m| m.len() > 0).unwrap_or(false) {
                best = Some(e.path());
            }
        }
    }
    best
}

fn find_cached(prefix: &str) -> Option<PathBuf> {
    let cache = paths::cache_dir().join("prime-mod");
    let Ok(rd) = fs::read_dir(cache) else {
        return None;
    };
    let mut best: Option<PathBuf> = None;
    for e in rd.flatten() {
        let name = e.file_name().to_string_lossy().to_string();
        if is_prime_jar_for_prefix(&name, prefix) {
            if fs::metadata(e.path()).map(|m| m.len() > 0).unwrap_or(false) {
                best = Some(e.path());
            }
        }
    }
    best
}

async fn download_from_github(prefix: &str, app: Option<&AppHandle>) -> Result<Option<PathBuf>, AppError> {
    emit_progress(app, "Checking GitHub Releases for Prime Client…", 28);
    let url = format!("https://api.github.com/repos/{OWNER}/{REPO}/releases/latest");
    let res = api_client()?
        .get(url)
        .header("Accept", "application/vnd.github+json")
        .send()
        .await
        .map_err(|e| {
            AppError::Message(format!(
                "GitHub unreachable while fetching Prime Client: {e}"
            ))
        })?;
    if !res.status().is_success() {
        return Ok(None);
    }
    let body: Value = res
        .json()
        .await
        .map_err(|e| AppError::Message(format!("Invalid GitHub release JSON: {e}")))?;
    let assets = body
        .get("assets")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default();
    let asset = assets.iter().find(|a| {
        a.get("name")
            .and_then(|n| n.as_str())
            .map(|n| is_prime_jar_for_prefix(n, prefix))
            .unwrap_or(false)
    });
    let Some(asset) = asset else {
        return Ok(None);
    };
    let name = asset
        .get("name")
        .and_then(|n| n.as_str())
        .ok_or_else(|| AppError::Message("Prime asset name missing".into()))?;
    let dl = asset
        .get("browser_download_url")
        .and_then(|u| u.as_str())
        .ok_or_else(|| AppError::Message("Prime download url missing".into()))?;
    emit_progress(
        app,
        &format!("Downloading {name} from GitHub…"),
        32,
    );
    let cache = paths::cache_dir().join("prime-mod");
    fs::create_dir_all(&cache)?;
    let dest = cache.join(name);
    download(dl, &dest).await?;
    Ok(Some(dest))
}

fn remove_stale_prime(mods: &Path, keep: &str) {
    if let Ok(rd) = fs::read_dir(mods) {
        for e in rd.flatten() {
            let name = e.file_name().to_string_lossy().to_string();
            if is_any_prime_jar(&name) && name != keep {
                let _ = fs::remove_file(e.path());
            }
        }
    }
}

pub async fn ensure_instance_mods(instance_id: &str, app: Option<&AppHandle>) -> Result<(), AppError> {
    let stored = instances::get_stored(instance_id)?
        .ok_or_else(|| AppError::Message("Instance not found".into()))?;
    if !stored.include_prime_mod || stored.loader != "fabric" {
        return Ok(());
    }

    let target = resolve_target(&stored.minecraft_version);
    let fabric_api = stored
        .fabric_api_version
        .as_deref()
        .unwrap_or(target.fabric_api);

    emit_progress(
        app,
        &format!("Preparing mods for Minecraft {}…", target.mc_version),
        14,
    );
    ensure_fabric_api(instance_id, &stored.minecraft_version, Some(fabric_api), app).await?;

    let mods = paths::instance_mods_dir(instance_id);
    fs::create_dir_all(&mods)?;

    let source = if let Ok(env) = std::env::var("PRIME_CLIENT_JAR") {
        let p = PathBuf::from(env);
        if p.exists() {
            Some(p)
        } else {
            None
        }
    } else {
        None
    }
    .or_else(|| find_local_build(&target))
    .or_else(|| find_installed(&mods, target.jar_prefix))
    .or_else(|| find_cached(target.jar_prefix));

    let source = match source {
        Some(p) => {
            emit_progress(
                app,
                &format!(
                    "Using Prime Client {}.",
                    p.file_name()
                        .and_then(|n| n.to_str())
                        .unwrap_or("jar")
                ),
                36,
            );
            p
        }
        None => match download_from_github(target.jar_prefix, app).await? {
            Some(p) => p,
            None => {
                emit_progress(
                    app,
                    "Prime Client jar not found on GitHub — launching with Fabric API only.",
                    36,
                );
                return Ok(());
            }
        },
    };

    let file_name = source
        .file_name()
        .and_then(|n| n.to_str())
        .unwrap_or("prime-client.jar")
        .to_string();
    remove_stale_prime(&mods, &file_name);
    let dest = mods.join(&file_name);
    if source != dest {
        emit_progress(app, &format!("Installing {file_name}…"), 40);
        fs::copy(&source, &dest)?;
    }
    emit_progress(app, "Mods ready.", 45);
    Ok(())
}

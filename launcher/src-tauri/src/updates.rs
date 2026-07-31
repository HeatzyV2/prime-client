use crate::error::AppError;
use crate::minecraft_targets::{is_prime_jar_for_prefix, DEFAULT_TARGET};
use crate::paths;
use parking_lot::Mutex;
use serde_json::{json, Value};
use std::fs;
use std::io::Write;
use std::path::PathBuf;
use std::time::{Duration, Instant};
use tauri::Emitter;

const OWNER: &str = "HeatzyV2";
const REPO: &str = "prime-client";
const CHECK_CACHE: Duration = Duration::from_secs(60 * 60);

static CACHED: Mutex<Option<(Instant, Value)>> = Mutex::new(None);

pub fn get_status() -> Option<Value> {
    CACHED.lock().as_ref().map(|(_, v)| v.clone())
}

pub async fn check(current_launcher: &str, force: bool) -> Result<Value, AppError> {
    if !force {
        if let Some((at, status)) = CACHED.lock().as_ref() {
            if at.elapsed() < CHECK_CACHE {
                return Ok(status.clone());
            }
        }
    }
    let status = fetch_status(current_launcher).await?;
    *CACHED.lock() = Some((Instant::now(), status.clone()));
    let mut s = crate::settings::load()?;
    s.last_update_check = Some(chrono::Utc::now().to_rfc3339());
    let _ = crate::settings::save(&s);
    Ok(status)
}

/// Matches Tauri NSIS (`Prime.Launcher_2.3.1_x64-setup.exe`) and legacy Electron
/// (`Prime-Launcher-Setup-2.2.0.exe`) installer names.
fn is_windows_launcher_asset(name: &str) -> bool {
    let lower = name.to_ascii_lowercase();
    if !lower.ends_with(".exe") || lower.contains("blockmap") {
        return false;
    }
    (lower.starts_with("prime.launcher_") && lower.contains("setup"))
        || lower.starts_with("prime-launcher-setup-")
        || (lower.starts_with("prime_launcher_") && lower.contains("setup"))
}

fn compare_semver_str(a: &str, b: &str) -> std::cmp::Ordering {
    let parse = |v: &str| -> Vec<u64> {
        v.trim_start_matches('v')
            .split('.')
            .filter_map(|p| p.parse().ok())
            .collect()
    };
    let pa = parse(a);
    let pb = parse(b);
    let len = pa.len().max(pb.len());
    for i in 0..len {
        let x = pa.get(i).copied().unwrap_or(0);
        let y = pb.get(i).copied().unwrap_or(0);
        match x.cmp(&y) {
            std::cmp::Ordering::Equal => {}
            other => return other,
        }
    }
    std::cmp::Ordering::Equal
}

async fn fetch_status(current_launcher: &str) -> Result<Value, AppError> {
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(30))
        .connect_timeout(Duration::from_secs(15))
        .user_agent("Prime-Launcher/2.3.2")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))?;
    let url = format!("https://api.github.com/repos/{OWNER}/{REPO}/releases/latest");
    let res = client
        .get(&url)
        .header("Accept", "application/vnd.github+json")
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    if !res.status().is_success() {
        return Ok(json!({
            "checkedAt": chrono::Utc::now().to_rfc3339(),
            "notes": "Offline — could not check GitHub Releases.",
            "releaseUrl": format!("https://github.com/{OWNER}/{REPO}/releases"),
            "anyUpdateAvailable": false,
            "launcher": { "current": current_launcher, "latest": current_launcher, "updateAvailable": false },
            "mod": { "current": "local", "latest": "local", "updateAvailable": false },
            "current": current_launcher,
            "latest": current_launcher,
            "updateAvailable": false
        }));
    }
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let tag = body
        .get("tag_name")
        .and_then(|v| v.as_str())
        .unwrap_or(current_launcher)
        .trim_start_matches('v')
        .to_string();
    let notes = body
        .get("body")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let assets = body
        .get("assets")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default();
    let launcher_asset = assets.iter().find(|a| {
        a.get("name")
            .and_then(|n| n.as_str())
            .map(is_windows_launcher_asset)
            .unwrap_or(false)
    });
    let preferred_prefix = DEFAULT_TARGET.jar_prefix;
    let mod_asset = assets
        .iter()
        .find(|a| {
            a.get("name")
                .and_then(|n| n.as_str())
                .map(|n| is_prime_jar_for_prefix(n, preferred_prefix))
                .unwrap_or(false)
        })
        .or_else(|| {
            assets.iter().find(|a| {
                a.get("name")
                    .and_then(|n| n.as_str())
                    .map(|n| n.starts_with("prime-client-") && n.ends_with(".jar"))
                    .unwrap_or(false)
            })
        });
    let launcher_url = launcher_asset
        .and_then(|a| a.get("browser_download_url").and_then(|u| u.as_str()))
        .map(str::to_string);
    let mod_url = mod_asset
        .and_then(|a| a.get("browser_download_url").and_then(|u| u.as_str()))
        .map(str::to_string);
    let mod_name = mod_asset
        .and_then(|a| a.get("name").and_then(|n| n.as_str()))
        .map(str::to_string);
    let launcher_update =
        compare_semver_str(&tag, current_launcher) == std::cmp::Ordering::Greater
            && launcher_url.is_some();
    // Mod jar presence alone is not an "update" — only when release tag is newer than current launcher tag context.
    // Always offer the jar download URL when present so Settings can reinstall; gate the badge on tag.
    let mod_update = mod_url.is_some()
        && compare_semver_str(&tag, current_launcher) == std::cmp::Ordering::Greater;
    Ok(json!({
        "checkedAt": chrono::Utc::now().to_rfc3339(),
        "notes": notes,
        "releaseUrl": format!("https://github.com/{OWNER}/{REPO}/releases"),
        "anyUpdateAvailable": launcher_update || mod_update,
        "launcher": {
            "current": current_launcher,
            "latest": tag,
            "updateAvailable": launcher_update,
            "downloadUrl": launcher_url,
            "fileName": launcher_asset.and_then(|a| a.get("name").and_then(|n| n.as_str()))
        },
        "mod": {
            "current": "local",
            "latest": tag,
            "updateAvailable": mod_update,
            "downloadUrl": mod_url,
            "fileName": mod_name
        },
        "current": current_launcher,
        "latest": tag,
        "updateAvailable": launcher_update
    }))
}

pub fn open_release(url: Option<String>) -> Result<(), AppError> {
    let target = url.unwrap_or_else(|| format!("https://github.com/{OWNER}/{REPO}/releases"));
    open::that(&target).map_err(|e| AppError::Message(e.to_string()))
}

pub async fn install_launcher(app: tauri::AppHandle, current: &str) -> Result<Value, AppError> {
    #[cfg(not(windows))]
    {
        let _ = (app, current);
        return Ok(json!({ "ok": false, "errorKey": "unsupported_platform" }));
    }
    #[cfg(windows)]
    {
        let status = check(current, true).await?;
        let launcher = status.get("launcher").cloned().unwrap_or(Value::Null);
        let available = launcher
            .get("updateAvailable")
            .and_then(|v| v.as_bool())
            .unwrap_or(false);
        let download_url = launcher
            .get("downloadUrl")
            .and_then(|v| v.as_str())
            .map(str::to_string);
        let file_name = launcher
            .get("fileName")
            .and_then(|v| v.as_str())
            .map(str::to_string)
            .unwrap_or_else(|| {
                format!(
                    "Prime.Launcher_{}_x64-setup.exe",
                    launcher
                        .get("latest")
                        .and_then(|v| v.as_str())
                        .unwrap_or("latest")
                )
            });
        let latest = launcher
            .get("latest")
            .and_then(|v| v.as_str())
            .unwrap_or(current)
            .to_string();

        if !available || download_url.is_none() {
            return Ok(json!({ "ok": false, "errorKey": "no_update" }));
        }
        let download_url = download_url.unwrap();

        let _ = app.emit(
            "update:progress",
            json!({ "target": "launcher", "phase": "downloading", "percent": 0, "detail": &file_name }),
        );

        let bytes = reqwest::Client::builder()
            .timeout(Duration::from_secs(300))
            .connect_timeout(Duration::from_secs(15))
            .user_agent("Prime-Launcher/2.3.2")
            .build()
            .map_err(|e| AppError::Message(e.to_string()))?
            .get(&download_url)
            .send()
            .await
            .map_err(|e| AppError::Message(format!("Launcher download failed: {e}")))?
            .error_for_status()
            .map_err(|e| AppError::Message(format!("Launcher download HTTP error: {e}")))?
            .bytes()
            .await
            .map_err(|e| AppError::Message(format!("Launcher download interrupted: {e}")))?;

        let dest = std::env::temp_dir().join(&file_name);
        fs::File::create(&dest)?.write_all(&bytes)?;

        let _ = app.emit(
            "update:progress",
            json!({ "target": "launcher", "phase": "installing", "percent": 100 }),
        );

        let exe = std::env::current_exe().map_err(|e| AppError::Message(e.to_string()))?;
        let install_dir = exe
            .parent()
            .map(|p| p.to_path_buf())
            .unwrap_or_else(|| PathBuf::from("."));
        let args = vec!["/S".to_string(), format!("/D={}", install_dir.display())];
        std::process::Command::new(&dest)
            .args(&args)
            .spawn()
            .map_err(|e| AppError::Message(e.to_string()))?;

        let app2 = app.clone();
        std::thread::spawn(move || {
            std::thread::sleep(std::time::Duration::from_millis(500));
            app2.exit(0);
        });

        Ok(json!({ "ok": true, "version": latest }))
    }
}

pub async fn install_mod(
    download_url: String,
    file_name: String,
    instance_id: String,
) -> Result<Value, AppError> {
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(120))
        .connect_timeout(Duration::from_secs(15))
        .user_agent("Prime-Launcher/2.3.2")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))?;
    let bytes = client
        .get(&download_url)
        .send()
        .await
        .map_err(|e| AppError::Message(format!("Mod download failed: {e}")))?
        .error_for_status()
        .map_err(|e| AppError::Message(format!("Mod download HTTP error: {e}")))?
        .bytes()
        .await
        .map_err(|e| AppError::Message(format!("Mod download interrupted: {e}")))?;
    let cache = paths::cache_dir().join("prime-mod");
    fs::create_dir_all(&cache)?;
    let cached = cache.join(&file_name);
    fs::File::create(&cached)?.write_all(&bytes)?;
    let mods = paths::instance_mods_dir(&instance_id);
    fs::create_dir_all(&mods)?;
    if let Ok(rd) = fs::read_dir(&mods) {
        for e in rd.flatten() {
            let n = e.file_name().to_string_lossy().to_string();
            if n.starts_with("prime-client-") && n.ends_with(".jar") {
                let _ = fs::remove_file(e.path());
            }
        }
    }
    fs::copy(&cached, mods.join(&file_name))?;
    Ok(json!({ "ok": true, "version": file_name }))
}

//! Microsoft Live → Xbox Live → Minecraft Services OAuth.
//! Matches Electron msmc: Mojang public client + login.live.com (not Azure Prism).
use crate::accounts::{load, save, StoredMinecraftAccount};
use crate::error::{AppError, OkResult};
use chrono::Utc;
use serde_json::{json, Value};
use std::sync::mpsc;
use std::time::Duration;
use tauri::{AppHandle, Manager, WebviewUrl, WebviewWindowBuilder};
use uuid::Uuid;

/// Mojang / official Minecraft launcher public client — same as msmc's default.
const CLIENT_ID: &str = "00000000402b5328";
/// Desktop redirect used by msmc `Auth.launch('electron')`.
const REDIRECT_URI: &str = "https://login.live.com/oauth20_desktop.srf";
const SCOPE: &str = "XboxLive.signin offline_access";
const LOGIN_WINDOW: &str = "ms-login";

fn skin_url(uuid: &str) -> String {
    format!("https://mc-heads.net/avatar/{}/64", uuid.replace('-', ""))
}

fn http_client() -> Result<reqwest::Client, AppError> {
    reqwest::Client::builder()
        .timeout(Duration::from_secs(60))
        .user_agent("Prime-Launcher/2.3.3")
        .build()
        .map_err(|e| AppError::Message(e.to_string()))
}

fn auth_url() -> String {
    format!(
        "https://login.live.com/oauth20_authorize.srf?client_id={}&response_type=code&redirect_uri={}&scope={}&prompt=select_account",
        CLIENT_ID,
        urlencoding::encode(REDIRECT_URI),
        urlencoding::encode(SCOPE),
    )
}

fn extract_code(url: &url::Url) -> Option<String> {
    url.query_pairs()
        .find(|(k, _)| k == "code")
        .map(|(_, v)| v.into_owned())
}

/// Interactive OAuth via an embedded webview (Electron msmc BrowserWindow equivalent).
pub fn login_interactive(app: AppHandle) -> Result<OkResult, AppError> {
    let (code_tx, code_rx) = mpsc::channel::<Option<String>>();
    let (built_tx, built_rx) = mpsc::channel::<Result<(), String>>();

    let app_build = app.clone();
    app.run_on_main_thread(move || {
        if let Some(existing) = app_build.get_webview_window(LOGIN_WINDOW) {
            let _ = existing.close();
        }

        let parsed = match auth_url().parse::<url::Url>() {
            Ok(u) => u,
            Err(e) => {
                let _ = built_tx.send(Err(e.to_string()));
                return;
            }
        };

        let app_nav = app_build.clone();
        let tx_nav = code_tx.clone();
        let tx_close = code_tx;

        let built = WebviewWindowBuilder::new(&app_build, LOGIN_WINDOW, WebviewUrl::External(parsed))
            .title("Sign in with Microsoft")
            .inner_size(520.0, 720.0)
            .center()
            .focused(true)
            .on_navigation(move |nav_url| {
                let href = nav_url.as_str();
                if !href.starts_with(REDIRECT_URI) {
                    return true;
                }
                let code = extract_code(&nav_url);
                let _ = tx_nav.send(code);
                if let Some(w) = app_nav.get_webview_window(LOGIN_WINDOW) {
                    let _ = w.close();
                }
                false
            })
            .build();

        match built {
            Ok(win) => {
                win.on_window_event(move |event| {
                    if let tauri::WindowEvent::Destroyed = event {
                        let _ = tx_close.send(None);
                    }
                });
                let _ = built_tx.send(Ok(()));
            }
            Err(e) => {
                let _ = built_tx.send(Err(e.to_string()));
            }
        }
    })
    .map_err(|e| AppError::Message(format!("Failed to open Microsoft login: {e}")))?;

    built_rx
        .recv_timeout(Duration::from_secs(15))
        .map_err(|_| AppError::Message("Failed to open Microsoft login window.".into()))?
        .map_err(AppError::Message)?;

    let code = code_rx
        .recv_timeout(Duration::from_secs(300))
        .map_err(|_| AppError::Message("Microsoft login timed out.".into()))?
        .ok_or_else(|| AppError::Message("Microsoft login cancelled or failed.".into()))?;

    // Drop late None from window Destroyed after a successful code.
    while let Ok(extra) = code_rx.try_recv() {
        if extra.is_some() {
            // ignore
        }
    }

    let tokens = block_on(exchange_code(&code))??;
    let account = tokens_to_account(tokens)?;
    let mut db = load()?;
    db.accounts.retain(|a| a.uuid != account.uuid);
    let id = account.id.clone();
    db.active_account_id = Some(id.clone());
    db.prime_account.username = account.username.clone();
    db.prime_account.tier = "prime".into();
    if let Some(p) = db.profiles.iter_mut().find(|p| p.id == db.active_profile_id) {
        p.minecraft_account_id = id.clone();
    }
    db.accounts.push(account);
    save(&db)?;
    Ok(OkResult {
        ok: true,
        error: None,
        message: None,
        account_id: Some(id),
    })
}

fn block_on<T>(fut: impl std::future::Future<Output = T>) -> Result<T, AppError> {
    if let Ok(handle) = tokio::runtime::Handle::try_current() {
        Ok(handle.block_on(fut))
    } else {
        Ok(tokio::runtime::Runtime::new()
            .map_err(|e| AppError::Message(e.to_string()))?
            .block_on(fut))
    }
}

struct MsTokens {
    access_token: String,
    refresh_token: String,
}

async fn exchange_code(code: &str) -> Result<MsTokens, AppError> {
    let client = http_client()?;
    let res = client
        .post("https://login.live.com/oauth20_token.srf")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(format!(
            "client_id={}&code={}&grant_type=authorization_code&redirect_uri={}",
            CLIENT_ID,
            urlencoding::encode(code),
            urlencoding::encode(REDIRECT_URI),
        ))
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let access = body
        .get("access_token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message(format!("Token error: {body}")))?
        .to_string();
    let refresh = body
        .get("refresh_token")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    Ok(MsTokens {
        access_token: access,
        refresh_token: refresh,
    })
}

async fn refresh_ms(refresh_token: &str) -> Result<MsTokens, AppError> {
    let client = http_client()?;
    let res = client
        .post("https://login.live.com/oauth20_token.srf")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(format!(
            "client_id={}&refresh_token={}&grant_type=refresh_token",
            CLIENT_ID,
            urlencoding::encode(refresh_token),
        ))
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let body: Value = res.json().await.map_err(|e| AppError::Message(e.to_string()))?;
    let access = body
        .get("access_token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            AppError::Message(format!(
                "Refresh failed — sign in again. ({})",
                body.get("error_description")
                    .or_else(|| body.get("error"))
                    .and_then(|v| v.as_str())
                    .unwrap_or("unknown")
            ))
        })?
        .to_string();
    let refresh = body
        .get("refresh_token")
        .and_then(|v| v.as_str())
        .unwrap_or(refresh_token)
        .to_string();
    Ok(MsTokens {
        access_token: access,
        refresh_token: refresh,
    })
}

async fn xbox_minecraft(
    ms_access: &str,
) -> Result<(String, String, String, Option<String>, Option<String>), AppError> {
    let client = http_client()?;
    let xbox_body = json!({
        "Properties": {
            "AuthMethod": "RPS",
            "SiteName": "user.auth.xboxlive.com",
            "RpsTicket": format!("d={ms_access}")
        },
        "RelyingParty": "http://auth.xboxlive.com",
        "TokenType": "JWT"
    });
    let xbox: Value = client
        .post("https://user.auth.xboxlive.com/user/authenticate")
        .json(&xbox_body)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let xbox_token = xbox
        .get("Token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Xbox auth failed.".into()))?;
    let uhs = xbox
        .pointer("/DisplayClaims/xui/0/uhs")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Xbox UHS missing.".into()))?;

    let xsts_body = json!({
        "Properties": {
            "SandboxId": "RETAIL",
            "UserTokens": [xbox_token]
        },
        "RelyingParty": "rp://api.minecraftservices.com/",
        "TokenType": "JWT"
    });
    let xsts: Value = client
        .post("https://xsts.auth.xboxlive.com/xsts/authorize")
        .json(&xsts_body)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let xsts_token = xsts
        .get("Token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("XSTS auth failed (no Minecraft?).".into()))?;

    let mc_login = json!({
        "identityToken": format!("XBL3.0 x={uhs};{xsts_token}")
    });
    let mc: Value = client
        .post("https://api.minecraftservices.com/authentication/login_with_xbox")
        .json(&mc_login)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let mc_token = mc
        .get("access_token")
        .and_then(|v| v.as_str())
        .ok_or_else(|| AppError::Message("Minecraft login failed.".into()))?
        .to_string();

    let profile: Value = client
        .get("https://api.minecraftservices.com/minecraft/profile")
        .bearer_auth(&mc_token)
        .send()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?
        .json()
        .await
        .map_err(|e| AppError::Message(e.to_string()))?;
    let uuid = profile
        .get("id")
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            AppError::Message("No Minecraft profile — buy the game on this account.".into())
        })?
        .to_string();
    let name = profile
        .get("name")
        .and_then(|v| v.as_str())
        .unwrap_or("Player")
        .to_string();
    let dashed = if uuid.contains('-') {
        uuid
    } else {
        format!(
            "{}-{}-{}-{}-{}",
            &uuid[0..8],
            &uuid[8..12],
            &uuid[12..16],
            &uuid[16..20],
            &uuid[20..32]
        )
    };
    let skin = profile
        .get("skins")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter()
                .find(|s| s.get("state").and_then(|x| x.as_str()) == Some("ACTIVE"))
        })
        .and_then(|s| s.get("url").and_then(|u| u.as_str()).map(str::to_string));
    let cape = profile
        .get("capes")
        .and_then(|v| v.as_array())
        .and_then(|arr| {
            arr.iter()
                .find(|s| s.get("state").and_then(|x| x.as_str()) == Some("ACTIVE"))
        })
        .and_then(|s| s.get("url").and_then(|u| u.as_str()).map(str::to_string));
    Ok((mc_token, dashed, name, skin, cape))
}

fn tokens_to_account(tokens: MsTokens) -> Result<StoredMinecraftAccount, AppError> {
    let (mc_token, uuid, name, skin, cape) =
        block_on(xbox_minecraft(&tokens.access_token))??;
    let _ = mc_token;
    Ok(StoredMinecraftAccount {
        id: Uuid::new_v4().to_string(),
        account_type: "microsoft".into(),
        username: name,
        uuid: uuid.clone(),
        skin_url: Some(skin.unwrap_or_else(|| skin_url(&uuid))),
        cape_url: cape,
        ms_refresh_token: Some(tokens.refresh_token),
        ms_auth_provider: Some("live".into()),
        added_at: Utc::now().to_rfc3339(),
        last_used_at: Some(Utc::now().to_rfc3339()),
    })
}

pub fn refresh_account(account_id: &str) -> Result<OkResult, AppError> {
    let mut db = load()?;
    let Some(account) = db.accounts.iter_mut().find(|a| a.id == account_id) else {
        return Ok(OkResult::err("Account not found."));
    };
    let Some(refresh) = account.ms_refresh_token.clone() else {
        return Ok(OkResult::err("No Microsoft refresh token — sign in again."));
    };
    let tokens = match block_on(refresh_ms(&refresh))? {
        Ok(t) => t,
        Err(e) => {
            return Ok(OkResult::err(format!(
                "Microsoft session expired — sign in again. ({e})"
            )));
        }
    };
    let (mc_token, uuid, name, skin, cape) =
        match block_on(xbox_minecraft(&tokens.access_token))? {
            Ok(v) => v,
            Err(e) => return Ok(OkResult::err(e.to_string())),
        };
    let _ = mc_token;
    account.username = name;
    account.uuid = uuid.clone();
    account.skin_url = Some(skin.unwrap_or_else(|| skin_url(&uuid)));
    account.cape_url = cape;
    account.ms_refresh_token = Some(tokens.refresh_token);
    account.ms_auth_provider = Some("live".into());
    account.last_used_at = Some(Utc::now().to_rfc3339());
    save(&db)?;
    Ok(OkResult::ok())
}

/// Build minecraft-java-core style authenticator JSON for the launch bridge.
pub async fn launch_authenticator(account: &StoredMinecraftAccount) -> Result<Value, AppError> {
    if account.account_type == "offline" {
        return Ok(json!({
            "name": account.username,
            "uuid": account.uuid.replace('-', ""),
            "access_token": "prime_offline_access_token",
            "client_token": account.id,
            "user_properties": "{}",
            "meta": { "type": "offline", "online": false }
        }));
    }
    let refresh = account.ms_refresh_token.as_deref().ok_or_else(|| {
        AppError::Message(
            "No Microsoft session — open Accounts and sign in with Microsoft again.".into(),
        )
    })?;

    let tokens = refresh_ms(refresh).await.map_err(|e| {
        AppError::Message(format!(
            "Microsoft session expired — sign in again. ({e})"
        ))
    })?;

    {
        let mut db = load()?;
        if let Some(a) = db.accounts.iter_mut().find(|a| a.id == account.id) {
            a.ms_refresh_token = Some(tokens.refresh_token.clone());
            a.ms_auth_provider = Some("live".into());
            save(&db)?;
        }
    }

    let (mc_token, uuid, name, _, _) = xbox_minecraft(&tokens.access_token).await?;
    Ok(json!({
        "name": name,
        "uuid": uuid.replace('-', ""),
        "access_token": mc_token,
        "client_token": account.id,
        "user_properties": "{}",
        "meta": { "type": "Xbox", "demo": false }
    }))
}

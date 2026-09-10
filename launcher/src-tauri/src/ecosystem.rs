use crate::error::AppError;
use crate::paths;
use crate::settings;
use crate::state::AppState;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use std::fs;
use std::time::Instant;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct EcosystemDb {
    pub version: u32,
    pub prime_coins: i64,
    pub owned_store_items: Vec<String>,
    pub equipped_cosmetics: Vec<String>,
    pub friends: Vec<Value>,
    pub favorite_servers: Vec<FavoriteServer>,
    #[serde(default)]
    pub store_history: Vec<StorePurchaseRecord>,
    #[serde(default)]
    pub redeemed_promos: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StorePurchaseRecord {
    pub id: String,
    pub item_id: String,
    pub item_name: String,
    pub price: i64,
    pub purchased_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FavoriteServer {
    pub id: String,
    pub name: String,
    pub address: String,
    pub players: u32,
    pub max_players: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ping: Option<u32>,
}

fn default_db() -> EcosystemDb {
    EcosystemDb {
        version: 1,
        prime_coins: 500,
        owned_store_items: vec!["cape-prime".into()],
        equipped_cosmetics: vec!["cape-prime".into(), "badge-veteran".into()],
        friends: vec![],
        favorite_servers: vec![],
        store_history: vec![],
        redeemed_promos: vec![],
    }
}

use parking_lot::Mutex;
static SYNC_MODE: Mutex<&'static str> = Mutex::new("local");

const LOCAL_PROMOS: &[(&str, i64, &str)] = &[
    ("PRIME2026", 100, "Prime 2026"),
    ("WELCOME", 50, "Welcome bonus"),
    ("ELYSIA", 75, "Elysia partner"),
];

const CLOUD_PROMOS: &[(&str, i64, &str)] = &[
    ("WELCOME100", 100, "Welcome bonus"),
    ("PRIME500", 500, "Prime starter pack"),
    ("ELYSIA250", 250, "Elysia promo"),
    ("FOUNDER1000", 1000, "Founder gift"),
];

fn set_sync_mode(mode: &'static str) {
    *SYNC_MODE.lock() = mode;
}

pub fn sync_mode() -> &'static str {
    *SYNC_MODE.lock()
}

fn map_cloud_history(history: &[Value]) -> Vec<StorePurchaseRecord> {
    let mut out = vec![];
    for h in history {
        if h.get("kind").and_then(|v| v.as_str()) != Some("purchase") {
            continue;
        }
        let Some(item_id) = h.get("itemId").and_then(|v| v.as_str()) else {
            continue;
        };
        let item_name = STORE
            .iter()
            .find(|(id, ..)| *id == item_id)
            .map(|(_, name, ..)| (*name).to_string())
            .unwrap_or_else(|| item_id.to_string());
        let amount = h.get("amount").and_then(|v| v.as_i64()).unwrap_or(0).abs();
        let fallback_id = Uuid::new_v4().to_string();
        out.push(StorePurchaseRecord {
            id: h
                .get("id")
                .and_then(|v| v.as_str())
                .unwrap_or(&fallback_id)
                .to_string(),
            item_id: item_id.to_string(),
            item_name,
            price: amount,
            purchased_at: h
                .get("createdAt")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string(),
        });
    }
    out.sort_by(|a, b| b.purchased_at.cmp(&a.purchased_at));
    out
}

fn redeemed_from_history(history: &[Value]) -> Vec<String> {
    let mut codes: Vec<String> = history
        .iter()
        .filter(|h| h.get("kind").and_then(|v| v.as_str()) == Some("redeem"))
        .filter_map(|h| {
            h.get("code")
                .and_then(|v| v.as_str())
                .map(|c| c.to_uppercase())
        })
        .collect();
    codes.sort();
    codes.dedup();
    codes
}

pub async fn store_catalog_cloud(state: &AppState) -> Result<Vec<Value>, AppError> {
    match crate::social::get_json_authed(state, "/v1/store/catalog").await {
        Ok(data) => {
            let balance = data.get("balance").and_then(|v| v.as_i64()).unwrap_or(0);
            let items = data
                .get("items")
                .and_then(|v| v.as_array())
                .cloned()
                .unwrap_or_default();
            let history = crate::social::get_json_authed(state, "/v1/store/history?limit=50")
                .await
                .ok()
                .and_then(|h| h.get("history").and_then(|v| v.as_array()).cloned())
                .unwrap_or_default();
            let owned: Vec<String> = items
                .iter()
                .filter(|i| i.get("owned").and_then(|v| v.as_bool()).unwrap_or(false))
                .filter_map(|i| i.get("id").and_then(|v| v.as_str()).map(str::to_string))
                .collect();
            let mut db = load()?;
            db.prime_coins = balance;
            db.owned_store_items = {
                let mut o = vec!["cape-prime".into()];
                o.extend(owned);
                o.sort();
                o.dedup();
                o
            };
            db.store_history = map_cloud_history(&history);
            db.redeemed_promos = redeemed_from_history(&history);
            save(&db)?;
            set_sync_mode("synced");
            Ok(items)
        }
        Err(_) => {
            set_sync_mode("local");
            store_catalog()
        }
    }
}

pub async fn store_balance_cloud(state: &AppState) -> Result<i64, AppError> {
    match crate::social::get_json_authed(state, "/v1/store/balance").await {
        Ok(data) => {
            let balance = data.get("balance").and_then(|v| v.as_i64()).unwrap_or(0);
            let mut db = load()?;
            db.prime_coins = balance;
            save(&db)?;
            set_sync_mode("synced");
            Ok(balance)
        }
        Err(_) => balance(),
    }
}

pub async fn store_history(state: &AppState) -> Result<Vec<Value>, AppError> {
    match crate::social::get_json_authed(state, "/v1/store/history?limit=50").await {
        Ok(data) => {
            let history = data
                .get("history")
                .and_then(|v| v.as_array())
                .cloned()
                .unwrap_or_default();
            let mapped = map_cloud_history(&history);
            let redeemed = redeemed_from_history(&history);
            let mut db = load()?;
            db.store_history = mapped.clone();
            db.redeemed_promos = redeemed;
            save(&db)?;
            set_sync_mode("synced");
            Ok(mapped
                .into_iter()
                .map(|h| {
                    json!({
                        "id": h.id,
                        "itemId": h.item_id,
                        "itemName": h.item_name,
                        "price": h.price,
                        "purchasedAt": h.purchased_at
                    })
                })
                .collect())
        }
        Err(_) => local_history(),
    }
}

fn local_history() -> Result<Vec<Value>, AppError> {
    set_sync_mode("local");
    let mut hist = load()?.store_history;
    hist.sort_by(|a, b| b.purchased_at.cmp(&a.purchased_at));
    Ok(hist
        .into_iter()
        .map(|h| {
            json!({
                "id": h.id,
                "itemId": h.item_id,
                "itemName": h.item_name,
                "price": h.price,
                "purchasedAt": h.purchased_at
            })
        })
        .collect())
}

pub async fn store_promos(state: &AppState) -> Result<Vec<Value>, AppError> {
    match crate::social::get_json_authed(state, "/v1/store/history?limit=50").await {
        Ok(data) => {
            let history = data
                .get("history")
                .and_then(|v| v.as_array())
                .cloned()
                .unwrap_or_default();
            let redeemed: std::collections::HashSet<String> =
                redeemed_from_history(&history).into_iter().collect();
            let mut db = load()?;
            db.redeemed_promos = redeemed.iter().cloned().collect();
            save(&db)?;
            set_sync_mode("synced");
            Ok(CLOUD_PROMOS
                .iter()
                .map(|(code, coins, label)| {
                    json!({
                        "code": code,
                        "label": label,
                        "coins": coins,
                        "redeemed": redeemed.contains(*code)
                    })
                })
                .collect())
        }
        Err(_) => local_promos(),
    }
}

fn local_promos() -> Result<Vec<Value>, AppError> {
    set_sync_mode("local");
    let db = load()?;
    let redeemed: std::collections::HashSet<_> =
        db.redeemed_promos.iter().cloned().collect();
    Ok(LOCAL_PROMOS
        .iter()
        .map(|(code, coins, label)| {
            json!({
                "code": code,
                "label": label,
                "coins": coins,
                "redeemed": redeemed.contains(*code)
            })
        })
        .collect())
}

pub async fn store_redeem(state: &AppState, code_raw: String) -> Result<Value, AppError> {
    match crate::social::post_json_authed(
        state,
        "/v1/store/redeem",
        json!({ "code": code_raw.clone() }),
    )
    .await
    {
        Ok(data) => {
            let balance = data.get("balance").and_then(|v| v.as_i64()).unwrap_or(0);
            let coins = data.get("coins").and_then(|v| v.as_i64()).unwrap_or(0);
            let code = data
                .get("code")
                .and_then(|v| v.as_str())
                .unwrap_or(&code_raw)
                .to_uppercase();
            let mut db = load()?;
            db.prime_coins = balance;
            if !db.redeemed_promos.contains(&code) {
                db.redeemed_promos.push(code);
            }
            save(&db)?;
            set_sync_mode("synced");
            Ok(json!({ "ok": true, "coins": coins }))
        }
        Err(e) => {
            let msg = e.to_string();
            if msg.contains("unavailable") || msg.contains("Auth") || msg.contains("session") {
                redeem_local(code_raw)
            } else {
                Ok(json!({ "ok": false, "error": msg }))
            }
        }
    }
}

fn redeem_local(code_raw: String) -> Result<Value, AppError> {
    set_sync_mode("local");
    let code = code_raw.trim().to_uppercase();
    let Some(&(_, coins, _)) = LOCAL_PROMOS.iter().find(|(c, ..)| *c == code) else {
        return Ok(json!({ "ok": false, "error": "Invalid promo code." }));
    };
    let mut db = load()?;
    if db.redeemed_promos.contains(&code) {
        return Ok(json!({ "ok": false, "error": "Promo already redeemed." }));
    }
    db.prime_coins += coins;
    db.redeemed_promos.push(code);
    save(&db)?;
    Ok(json!({ "ok": true, "coins": coins }))
}

pub async fn store_purchase_cloud(state: &AppState, item_id: String) -> Result<Value, AppError> {
    match crate::social::post_json_authed(
        state,
        "/v1/store/purchase",
        json!({ "itemId": item_id.clone() }),
    )
    .await
    {
        Ok(data) => {
            let balance = data.get("balance").and_then(|v| v.as_i64()).unwrap_or(0);
            let price = data.get("price").and_then(|v| v.as_i64()).unwrap_or(0);
            let owned = data
                .get("owned")
                .and_then(|v| v.as_array())
                .cloned()
                .unwrap_or_default();
            let item = STORE.iter().find(|(id, ..)| *id == item_id);
            let mut db = load()?;
            db.prime_coins = balance;
            db.owned_store_items = {
                let mut o = vec!["cape-prime".into()];
                for v in &owned {
                    if let Some(s) = v.as_str() {
                        o.push(s.to_string());
                    }
                }
                o.sort();
                o.dedup();
                o
            };
            if let Some(&(id, name, _, _, _)) = item {
                if !db.store_history.iter().any(|h| h.item_id == id) {
                    db.store_history.push(StorePurchaseRecord {
                        id: Uuid::new_v4().to_string(),
                        item_id: id.into(),
                        item_name: name.into(),
                        price,
                        purchased_at: chrono::Utc::now().to_rfc3339(),
                    });
                }
            }
            save(&db)?;
            if item_id == "bg-nebula" {
                let _ = settings::update_merge(json!({ "backgroundNebula": true }));
            }
            if item_id == "theme-crimson" {
                let _ = settings::update_merge(json!({ "theme": "prime-crimson" }));
            }
            crate::bridge::sync_all_prime_instances()?;
            set_sync_mode("synced");
            Ok(json!({ "ok": true, "balance": balance }))
        }
        Err(e) => {
            let msg = e.to_string();
            if msg.contains("Auth") || msg.contains("unavailable") || msg.contains("session") {
                purchase(item_id)
            } else {
                Ok(json!({ "ok": false, "error": msg }))
            }
        }
    }
}

pub fn load() -> Result<EcosystemDb, AppError> {
    let path = paths::ecosystem_path();
    if !path.exists() {
        let db = default_db();
        save(&db)?;
        return Ok(db);
    }
    let raw = fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&raw).unwrap_or_else(|_| default_db()))
}

pub fn save(db: &EcosystemDb) -> Result<(), AppError> {
    let path = paths::ecosystem_path();
    if let Some(p) = path.parent() {
        fs::create_dir_all(p)?;
    }
    fs::write(path, serde_json::to_string_pretty(db)?)?;
    Ok(())
}

const STORE: &[(&str, &str, &str, i64, &str)] = &[
    ("cape-prime", "Prime Cape", "Official Prime Client cape.", 0, "cosmetic"),
    ("theme-crimson", "Crimson Theme", "Signature red Prime theme.", 0, "theme"),
    ("bg-nebula", "Nebula Background", "Animated space background.", 150, "background"),
    ("badge-founder", "Founder Badge", "Limited edition profile badge.", 500, "badge"),
    ("wings-ember", "Ember Wings", "Fiery cosmetic wings.", 400, "cosmetic"),
    ("pet-fox", "Arctic Fox", "Companion pet cosmetic.", 300, "cosmetic"),
    ("emote-wave", "Prime Wave", "Signature emote.", 100, "cosmetic"),
];

const STORE_TO_COSMETIC: &[(&str, &str)] = &[
    ("cape-prime", "cape-prime"),
    ("wings-ember", "wings-ember"),
    ("pet-fox", "pet-fox"),
    ("emote-wave", "emote-wave"),
    ("badge-founder", "badge-founder"),
];

const COSMETICS: &[(&str, &str, &str, &str)] = &[
    ("cape-prime", "Prime Cape", "cape", "legendary"),
    ("wings-ember", "Ember Wings", "wings", "epic"),
    ("pet-fox", "Arctic Fox", "pet", "rare"),
    ("emote-wave", "Prime Wave", "emote", "common"),
    ("badge-founder", "Founder", "badge", "legendary"),
    ("badge-veteran", "Veteran", "badge", "rare"),
];

pub fn store_catalog() -> Result<Vec<Value>, AppError> {
    let db = load()?;
    Ok(STORE
        .iter()
        .map(|(id, name, desc, price, cat)| {
            json!({
                "id": id,
                "name": name,
                "description": desc,
                "price": price,
                "category": cat,
                "owned": db.owned_store_items.iter().any(|x| x == id)
            })
        })
        .collect())
}

pub fn balance() -> Result<i64, AppError> {
    Ok(load()?.prime_coins)
}

pub fn purchase(item_id: String) -> Result<Value, AppError> {
    let Some(&(id, name, _, price, _)) = STORE.iter().find(|(i, ..)| *i == item_id) else {
        return Ok(json!({ "ok": false, "error": "Unknown item." }));
    };
    let mut db = load()?;
    if db.owned_store_items.iter().any(|x| x == id) {
        return Ok(json!({ "ok": false, "error": "Already owned." }));
    }
    if db.prime_coins < price {
        return Ok(json!({ "ok": false, "error": "Not enough Prime Coins." }));
    }
    db.prime_coins -= price;
    db.owned_store_items.push(id.to_string());
    if !db.store_history.iter().any(|h| h.item_id == id) {
        db.store_history.push(StorePurchaseRecord {
            id: Uuid::new_v4().to_string(),
            item_id: id.to_string(),
            item_name: name.to_string(),
            price,
            purchased_at: chrono::Utc::now().to_rfc3339(),
        });
    }
    if let Some((_, cos)) = STORE_TO_COSMETIC.iter().find(|(s, _)| *s == id) {
        if !db.equipped_cosmetics.iter().any(|x| x == *cos) {
            // don't auto-equip all; just ensure owned via store
        }
    }
    save(&db)?;
    set_sync_mode("local");
    if id == "bg-nebula" {
        let _ = settings::update_merge(json!({ "backgroundNebula": true }));
    }
    if id == "theme-crimson" {
        let _ = settings::update_merge(json!({ "theme": "prime-crimson" }));
    }
    crate::bridge::sync_all_prime_instances()?;
    Ok(json!({ "ok": true, "balance": db.prime_coins }))
}

pub fn cosmetic_list() -> Result<Vec<Value>, AppError> {
    let db = load()?;
    let mut owned: Vec<String> = vec!["badge-veteran".into()];
    for (store, cos) in STORE_TO_COSMETIC {
        if db.owned_store_items.iter().any(|x| x == store) {
            owned.push((*cos).into());
        }
    }
    if db.owned_store_items.iter().any(|x| x == "cape-prime") {
        owned.push("cape-prime".into());
    }
    owned.sort();
    owned.dedup();
    Ok(COSMETICS
        .iter()
        .filter(|(id, ..)| owned.iter().any(|o| o == id))
        .map(|(id, name, ty, rarity)| {
            json!({
                "id": id,
                "name": name,
                "type": ty,
                "rarity": rarity,
                "equipped": db.equipped_cosmetics.iter().any(|e| e == id)
            })
        })
        .collect())
}

pub fn cosmetic_toggle(cosmetic_id: String) -> Result<Value, AppError> {
    let mut db = load()?;
    let owned = cosmetic_list()?;
    if !owned.iter().any(|c| c.get("id").and_then(|v| v.as_str()) == Some(&cosmetic_id)) {
        return Ok(json!({ "ok": false, "error": "Not owned." }));
    }
    let ty = COSMETICS
        .iter()
        .find(|(id, ..)| *id == cosmetic_id)
        .map(|(_, _, t, _)| *t)
        .unwrap_or("badge");
    if db.equipped_cosmetics.iter().any(|e| e == &cosmetic_id) {
        db.equipped_cosmetics.retain(|e| e != &cosmetic_id);
    } else {
        if ty != "badge" {
            let same_type: Vec<String> = COSMETICS
                .iter()
                .filter(|(_, _, t, _)| *t == ty)
                .map(|(id, ..)| (*id).to_string())
                .collect();
            db.equipped_cosmetics.retain(|e| !same_type.contains(e));
        }
        db.equipped_cosmetics.push(cosmetic_id);
    }
    save(&db)?;
    crate::bridge::sync_all_prime_instances()?;
    Ok(json!({ "ok": true }))
}

pub fn servers_list() -> Result<Vec<FavoriteServer>, AppError> {
    Ok(load()?.favorite_servers)
}

fn parse_address(address: &str) -> Result<(String, u16), AppError> {
    let address = address.trim();
    if address.is_empty() || address.len() > 255 {
        return Err(AppError::Message("Invalid address.".into()));
    }
    if let Some((host, port)) = address.rsplit_once(':') {
        if let Ok(p) = port.parse::<u16>() {
            return Ok((host.to_string(), p));
        }
    }
    Ok((address.to_string(), 25565))
}

pub async fn servers_add(name: String, address: String) -> Result<Value, AppError> {
    let name = name.trim().to_string();
    if name.is_empty() || name.len() > 48 {
        return Ok(json!({ "ok": false, "error": "Name must be 1–48 characters." }));
    }
    let (host, port) = match parse_address(&address) {
        Ok(v) => v,
        Err(e) => return Ok(json!({ "ok": false, "error": e.to_string() })),
    };
    let mut db = load()?;
    let mut server = FavoriteServer {
        id: Uuid::new_v4().to_string(),
        name,
        address: format!("{host}:{port}"),
        players: 0,
        max_players: 0,
        ping: None,
    };
    refresh_one(&mut server).await;
    db.favorite_servers.push(server);
    save(&db)?;
    Ok(json!({ "ok": true }))
}

pub fn servers_remove(server_id: String) -> Result<Value, AppError> {
    let mut db = load()?;
    db.favorite_servers.retain(|s| s.id != server_id);
    save(&db)?;
    Ok(json!({ "ok": true }))
}

async fn refresh_one(server: &mut FavoriteServer) {
    let Ok((host, port)) = parse_address(&server.address) else {
        return;
    };
    let url = format!("https://api.mcstatus.io/v2/status/java/{host}:{port}");
    let start = Instant::now();
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .user_agent("Prime-Launcher")
        .build();
    let Ok(client) = client else { return };
    match client.get(&url).send().await {
        Ok(res) if res.status().is_success() => {
            if let Ok(body) = res.json::<Value>().await {
                let online = body.get("online").and_then(|v| v.as_bool()).unwrap_or(false);
                if online {
                    server.players = body
                        .pointer("/players/online")
                        .and_then(|v| v.as_u64())
                        .unwrap_or(0) as u32;
                    server.max_players = body
                        .pointer("/players/max")
                        .and_then(|v| v.as_u64())
                        .unwrap_or(0) as u32;
                    server.ping = Some(start.elapsed().as_millis() as u32);
                } else {
                    server.players = 0;
                    server.max_players = 0;
                    server.ping = None;
                }
            }
        }
        _ => {
            server.players = 0;
            server.max_players = 0;
            server.ping = None;
        }
    }
}

pub async fn servers_refresh(server_id: String) -> Result<Option<FavoriteServer>, AppError> {
    let mut db = load()?;
    if let Some(s) = db.favorite_servers.iter_mut().find(|s| s.id == server_id) {
        refresh_one(s).await;
        let out = s.clone();
        save(&db)?;
        return Ok(Some(out));
    }
    Ok(None)
}

pub async fn servers_refresh_all() -> Result<Vec<FavoriteServer>, AppError> {
    let mut db = load()?;
    for s in &mut db.favorite_servers {
        refresh_one(s).await;
    }
    save(&db)?;
    Ok(db.favorite_servers)
}

pub fn reward_launch_coins() -> Result<(), AppError> {
    let mut db = load()?;
    db.prime_coins += 10;
    save(&db)
}

pub fn equipped_for_bridge() -> Result<Vec<String>, AppError> {
    Ok(load()?.equipped_cosmetics)
}

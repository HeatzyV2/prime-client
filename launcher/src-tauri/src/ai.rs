//! Prime AI assistant — Groq via backend proxy or local key (parity with AiAssistantService).
use crate::content;
use crate::error::AppError;
use crate::instances;
use crate::logs;
use crate::paths;
use serde_json::{json, Value};
use std::fs;

const MODEL: &str = "llama-3.3-70b-versatile";
const MAX_TOOL_ROUNDS: usize = 6;
const MAX_HISTORY: usize = 12;
const DEFAULT_API: &str = "http://194.9.172.102:26005";

fn api_base() -> String {
    std::env::var("PRIME_API_BASE").unwrap_or_else(|_| DEFAULT_API.into())
}

fn secrets_path() -> std::path::PathBuf {
    paths::user_data_dir().join("secrets.json")
}

fn load_secrets() -> Value {
    fs::read_to_string(secrets_path())
        .ok()
        .and_then(|r| serde_json::from_str(&r).ok())
        .unwrap_or(json!({}))
}

fn save_secrets(v: &Value) -> Result<(), AppError> {
    fs::create_dir_all(paths::user_data_dir())?;
    fs::write(secrets_path(), serde_json::to_string_pretty(v)?)?;
    Ok(())
}

fn groq_key() -> String {
    let file = load_secrets();
    let from_file = file
        .get("groqApiKey")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_string();
    if !from_file.is_empty() {
        return from_file;
    }
    std::env::var("GROQ_API_KEY").unwrap_or_default().trim().to_string()
}

fn mask_api_key(key: &str) -> String {
    let trimmed = key.trim();
    if trimmed.len() < 12 {
        return "••••".into();
    }
    format!("{}…{}", &trimmed[..6], &trimmed[trimmed.len() - 4..])
}

pub async fn proxy_available() -> bool {
    let client = reqwest::Client::new();
    match client
        .get(format!("{}/v1/ai/status", api_base()))
        .timeout(std::time::Duration::from_secs(5))
        .send()
        .await
    {
        Ok(res) if res.status().is_success() => {
            let body: Value = res.json().await.unwrap_or(Value::Null);
            body.get("available").and_then(|v| v.as_bool()).unwrap_or(false)
        }
        _ => false,
    }
}

pub async fn key_status() -> Result<Value, AppError> {
    let via_proxy = proxy_available().await;
    let local = groq_key();
    Ok(json!({
        "hasKey": via_proxy || !local.is_empty(),
        "maskedKey": if local.is_empty() { Value::Null } else { json!(mask_api_key(&local)) },
        "viaProxy": via_proxy
    }))
}

pub async fn set_key(key: String) -> Result<Value, AppError> {
    let trimmed = key.trim().to_string();
    if !trimmed.is_empty() && !trimmed.starts_with("gsk_") {
        return Err(AppError::Message(
            "Invalid Groq key (must start with gsk_)".into(),
        ));
    }
    let mut secrets = load_secrets();
    if let Some(obj) = secrets.as_object_mut() {
        obj.insert("groqApiKey".into(), json!(trimmed));
    }
    save_secrets(&secrets)?;
    key_status().await
}

pub async fn clear_key() -> Result<Value, AppError> {
    set_key(String::new()).await
}

fn system_prompt() -> &'static str {
    "Tu es Prime Assistant, l'IA du launcher Prime Client (Minecraft Fabric).\n\
     Tu aides à: installer des mods (Modrinth/CurseForge), conseils FPS/packs, ET dépanner crashes.\n\
     Utilise search_mods / propose_install ; l'utilisateur confirme dans l'UI.\n\
     Prefère Modrinth. Réponds en français, concis et actionnable."
}

fn tool_definitions() -> Value {
    json!([
        {
            "type": "function",
            "function": {
                "name": "get_instance_context",
                "description": "Get the active Minecraft instance name, version, and loader.",
                "parameters": {
                    "type": "object",
                    "properties": { "instanceId": { "type": "string" } }
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "list_installed_mods",
                "description": "List mods already installed on the active instance.",
                "parameters": {
                    "type": "object",
                    "properties": { "instanceId": { "type": "string" } }
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "search_mods",
                "description": "Search Modrinth for Fabric mods.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "query": { "type": "string" },
                        "instanceId": { "type": "string" }
                    },
                    "required": ["query"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "propose_install",
                "description": "Propose a mod/pack/shader install for UI confirmation.",
                "parameters": {
                    "type": "object",
                    "properties": {
                        "projectId": { "type": "string" },
                        "title": { "type": "string" },
                        "source": { "type": "string", "enum": ["modrinth", "curseforge"] },
                        "type": { "type": "string", "enum": ["mod", "resourcepack", "shader"] }
                    },
                    "required": ["projectId", "title"]
                }
            }
        },
        {
            "type": "function",
            "function": {
                "name": "read_launcher_log",
                "description": "Read recent Prime Launcher launch log lines.",
                "parameters": {
                    "type": "object",
                    "properties": { "limit": { "type": "number" } }
                }
            }
        }
    ])
}

fn coerce_proposal(item: &Value) -> Option<Value> {
    let project_id = item
        .get("projectId")
        .or(item.get("project_id"))
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_string();
    let title = item
        .get("title")
        .or(item.get("name"))
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_string();
    if project_id.is_empty() || title.is_empty() {
        return None;
    }
    let source = item
        .get("source")
        .and_then(|v| v.as_str())
        .unwrap_or("modrinth");
    let ty = item
        .get("type")
        .or(item.get("project_type"))
        .and_then(|v| v.as_str())
        .unwrap_or("mod");
    Some(json!({
        "projectId": project_id,
        "title": title,
        "source": if source == "curseforge" { "curseforge" } else { "modrinth" },
        "type": match ty {
            "resourcepack" | "resource_pack" => "resourcepack",
            "shader" => "shader",
            _ => "mod"
        },
        "description": item.get("description"),
        "downloads": item.get("downloads"),
        "iconUrl": item.get("iconUrl").or(item.get("icon_url")),
        "slug": item.get("slug")
    }))
}

fn normalize_proposals(raw: &[Value]) -> Vec<Value> {
    let mut by_key = std::collections::HashMap::new();
    for item in raw {
        if let Some(p) = coerce_proposal(item) {
            let key = format!(
                "{}:{}:{}",
                p.get("source").and_then(|v| v.as_str()).unwrap_or(""),
                p.get("type").and_then(|v| v.as_str()).unwrap_or(""),
                p.get("projectId").and_then(|v| v.as_str()).unwrap_or("")
            );
            by_key.insert(key, p);
        }
    }
    by_key.into_values().collect()
}

async fn resolve_instance_id(instance_id: Option<&str>) -> Option<String> {
    if let Some(id) = instance_id.filter(|s| !s.is_empty()) {
        return Some(id.to_string());
    }
    instances::get_default()
        .ok()
        .flatten()
        .and_then(|v| v.get("id").and_then(|x| x.as_str()).map(str::to_string))
}

async fn execute_tool(name: &str, args: &Value, instance_id: Option<&str>) -> (Value, Vec<Value>) {
    let mut proposals = vec![];
    let payload = match name {
        "get_instance_context" => {
            let id = resolve_instance_id(
                args.get("instanceId")
                    .and_then(|v| v.as_str())
                    .or(instance_id),
            )
            .await;
            match id.and_then(|i| instances::get(&i).ok().flatten()) {
                Some(inst) => inst,
                None => json!({ "error": "No instance" }),
            }
        }
        "list_installed_mods" => {
            let id = resolve_instance_id(
                args.get("instanceId")
                    .and_then(|v| v.as_str())
                    .or(instance_id),
            )
            .await;
            match content::list_mods(id).await {
                Ok(mods) => json!({ "mods": mods }),
                Err(e) => json!({ "error": e.to_string() }),
            }
        }
        "search_mods" => {
            let query = args.get("query").and_then(|v| v.as_str()).unwrap_or("");
            let id = resolve_instance_id(
                args.get("instanceId")
                    .and_then(|v| v.as_str())
                    .or(instance_id),
            )
            .await;
            match content::search_modrinth(query.to_string(), "mod".into(), id).await {
                Ok(hits) => {
                    for hit in hits.iter().take(8) {
                        if let Some(p) = coerce_proposal(&json!({
                            "projectId": hit.get("projectId").or(hit.get("project_id")).or(hit.get("id")),
                            "title": hit.get("title").or(hit.get("name")),
                            "source": "modrinth",
                            "type": "mod",
                            "description": hit.get("description"),
                            "downloads": hit.get("downloads"),
                            "iconUrl": hit.get("iconUrl").or(hit.get("icon_url")),
                            "slug": hit.get("slug")
                        })) {
                            proposals.push(p);
                        }
                    }
                    json!({ "results": hits })
                }
                Err(e) => json!({ "error": e.to_string() }),
            }
        }
        "propose_install" => {
            if let Some(p) = coerce_proposal(args) {
                proposals.push(p.clone());
                json!({ "proposed": p })
            } else {
                json!({ "error": "Invalid proposal" })
            }
        }
        "read_launcher_log" => {
            let limit = args.get("limit").and_then(|v| v.as_u64()).unwrap_or(40) as usize;
            let entries = logs::list();
            let start = entries.len().saturating_sub(limit);
            json!({ "lines": &entries[start..] })
        }
        _ => json!({ "error": format!("Unknown tool: {name}") }),
    };
    (payload, proposals)
}

async fn groq_chat(messages: &Value, api_key: Option<&str>) -> Result<Value, String> {
    let tools = tool_definitions();
    let body = json!({
        "model": MODEL,
        "messages": messages,
        "temperature": 0.3,
        "max_tokens": 1200,
        "tools": tools,
        "tool_choice": "auto"
    });

    let client = reqwest::Client::new();
    if let Some(key) = api_key.filter(|k| !k.is_empty()) {
        let res = client
            .post("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", format!("Bearer {key}"))
            .header("Content-Type", "application/json")
            .json(&body)
            .send()
            .await
            .map_err(|e| e.to_string())?;
        let status = res.status();
        let text = res.text().await.map_err(|e| e.to_string())?;
        if !status.is_success() {
            return Err(extract_error(&text));
        }
        let json: Value = serde_json::from_str(&text).map_err(|e| e.to_string())?;
        return json
            .pointer("/choices/0/message")
            .cloned()
            .ok_or_else(|| "Empty Groq response".into());
    }

    let res = client
        .post(format!("{}/v1/ai/chat", api_base()))
        .header("Content-Type", "application/json")
        .json(&body)
        .send()
        .await
        .map_err(|e| e.to_string())?;
    let status = res.status();
    let text = res.text().await.map_err(|e| e.to_string())?;
    if !status.is_success() {
        return Err(extract_error(&text));
    }
    let json: Value = serde_json::from_str(&text).map_err(|e| e.to_string())?;
    json.get("message")
        .cloned()
        .ok_or_else(|| json.get("error").and_then(|v| v.as_str()).unwrap_or("Empty AI response").to_string())
}

fn extract_error(raw: &str) -> String {
    if let Ok(json) = serde_json::from_str::<Value>(raw) {
        if let Some(s) = json.get("error").and_then(|v| v.as_str()) {
            return s.to_string();
        }
        if let Some(s) = json.pointer("/error/message").and_then(|v| v.as_str()) {
            return s.to_string();
        }
    }
    if raw.len() > 180 {
        format!("{}…", &raw[..180])
    } else {
        raw.to_string()
    }
}

pub async fn chat(payload: Value) -> Result<Value, AppError> {
    let status = key_status().await?;
    let has_key = status.get("hasKey").and_then(|v| v.as_bool()).unwrap_or(false);
    let via_proxy = status.get("viaProxy").and_then(|v| v.as_bool()).unwrap_or(false);
    if !has_key {
        return Ok(json!({
            "ok": false,
            "reply": "",
            "proposals": [],
            "hasKey": false,
            "viaProxy": false,
            "error": "AI indisponible (backend hors ligne). Réessaie plus tard."
        }));
    }
    let message = payload
        .get("message")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_string();
    if message.is_empty() {
        return Ok(json!({
            "ok": false,
            "reply": "",
            "proposals": [],
            "hasKey": true,
            "viaProxy": via_proxy,
            "error": "Empty message"
        }));
    }
    let instance_id = payload
        .get("instanceId")
        .and_then(|v| v.as_str())
        .map(str::to_string);
    let history = payload
        .get("history")
        .and_then(|v| v.as_array())
        .cloned()
        .unwrap_or_default();
    let history: Vec<_> = history
        .into_iter()
        .rev()
        .take(MAX_HISTORY)
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .filter(|h| {
            h.get("content")
                .and_then(|v| v.as_str())
                .map(|s| !s.trim().is_empty())
                .unwrap_or(false)
        })
        .collect();

    let mut messages = vec![json!({ "role": "system", "content": system_prompt() })];
    for h in &history {
        messages.push(json!({
            "role": h.get("role").and_then(|v| v.as_str()).unwrap_or("user"),
            "content": h.get("content").and_then(|v| v.as_str()).unwrap_or("")
        }));
    }
    messages.push(json!({ "role": "user", "content": message }));

    let local_key = if via_proxy {
        None
    } else {
        let k = groq_key();
        if k.is_empty() {
            None
        } else {
            Some(k)
        }
    };

    let mut collected: Vec<Value> = vec![];
    for _ in 0..MAX_TOOL_ROUNDS {
        let assistant = match groq_chat(&Value::Array(messages.clone()), local_key.as_deref()).await
        {
            Ok(m) => m,
            Err(e) => {
                return Ok(json!({
                    "ok": false,
                    "reply": "",
                    "proposals": normalize_proposals(&collected),
                    "hasKey": true,
                    "viaProxy": via_proxy,
                    "error": e
                }));
            }
        };
        messages.push(assistant.clone());
        let tool_calls = assistant
            .get("tool_calls")
            .and_then(|v| v.as_array())
            .cloned()
            .unwrap_or_default();
        if tool_calls.is_empty() {
            let reply = assistant
                .get("content")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .trim();
            return Ok(json!({
                "ok": true,
                "reply": if reply.is_empty() { "OK." } else { reply },
                "proposals": normalize_proposals(&collected),
                "hasKey": true,
                "viaProxy": via_proxy
            }));
        }
        for call in tool_calls {
            let name = call
                .pointer("/function/name")
                .and_then(|v| v.as_str())
                .unwrap_or("");
            let args_raw = call
                .pointer("/function/arguments")
                .and_then(|v| v.as_str())
                .unwrap_or("{}");
            let args: Value = serde_json::from_str(args_raw).unwrap_or(json!({}));
            let call_id = call.get("id").and_then(|v| v.as_str()).unwrap_or("");
            let (payload_out, props) = execute_tool(name, &args, instance_id.as_deref()).await;
            collected.extend(props);
            messages.push(json!({
                "role": "tool",
                "tool_call_id": call_id,
                "content": payload_out.to_string()
            }));
        }
    }
    Ok(json!({
        "ok": true,
        "reply": "J'ai trouvé des résultats — confirme les installations ci-dessous.",
        "proposals": normalize_proposals(&collected),
        "hasKey": true,
        "viaProxy": via_proxy
    }))
}

pub async fn confirm_install(payload: Value) -> Result<Value, AppError> {
    let project_id = payload
        .get("projectId")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_string();
    let title = payload
        .get("title")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_string();
    if project_id.is_empty() || title.is_empty() {
        return Ok(json!({ "ok": false, "error": "Invalid install request" }));
    }
    let source = payload
        .get("source")
        .and_then(|v| v.as_str())
        .unwrap_or("modrinth");
    let ty = payload
        .get("type")
        .and_then(|v| v.as_str())
        .unwrap_or("mod");
    let instance_id = payload
        .get("instanceId")
        .and_then(|v| v.as_str())
        .map(str::to_string);
    let version_id = payload
        .get("versionId")
        .and_then(|v| v.as_str())
        .map(str::to_string);

    match ty {
        "resourcepack" => {
            if source == "curseforge" {
                content::install_curseforge(project_id, title, instance_id, version_id, "resourcepack")
                    .await
            } else {
                content::install_modrinth(project_id, title, instance_id, version_id, "resourcepack")
                    .await
            }
        }
        "shader" => {
            if source == "curseforge" {
                content::install_curseforge(project_id, title, instance_id, version_id, "shader")
                    .await
            } else {
                content::install_modrinth(project_id, title, instance_id, version_id, "shader")
                    .await
            }
        }
        _ => {
            if source == "curseforge" {
                content::install_curseforge(project_id, title, instance_id, version_id, "mod").await
            } else {
                content::install_modrinth_mod(project_id, title, instance_id, version_id).await
            }
        }
    }
}

//! Crash report analysis — parity with CrashAnalyzerService.
use crate::error::AppError;
use serde_json::{json, Value};
use std::collections::HashSet;
use std::fs;
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Default)]
struct ParsedCrash {
    description: Option<String>,
    exception_type: Option<String>,
    exception_message: Option<String>,
    screen: Option<String>,
    prime_frames: Vec<(String, Option<u32>)>,
    mod_ids: Vec<String>,
}

pub fn snapshot_crash_reports(game_dir: &Path) -> HashSet<String> {
    let dir = game_dir.join("crash-reports");
    let Ok(entries) = fs::read_dir(dir) else {
        return HashSet::new();
    };
    entries
        .flatten()
        .filter_map(|e| {
            let name = e.file_name().to_string_lossy().to_string();
            if name.ends_with(".txt") {
                Some(name)
            } else {
                None
            }
        })
        .collect()
}

fn find_new_crash_report(
    game_dir: &Path,
    known: &HashSet<String>,
    session_started_ms: u128,
) -> Option<PathBuf> {
    let dir = game_dir.join("crash-reports");
    let entries = fs::read_dir(&dir).ok()?;
    let mut best: Option<(PathBuf, u128)> = None;
    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().to_string();
        if !name.ends_with(".txt") {
            continue;
        }
        let meta = entry.metadata().ok()?;
        let mtime = meta
            .modified()
            .ok()
            .and_then(|t| t.duration_since(UNIX_EPOCH).ok())
            .map(|d| d.as_millis())
            .unwrap_or(0);
        let is_new = !known.contains(&name) || mtime >= session_started_ms.saturating_sub(2000);
        if is_new && best.as_ref().map(|(_, t)| mtime >= *t).unwrap_or(true) {
            best = Some((entry.path(), mtime));
        }
    }
    best.map(|(p, _)| p)
}

fn looks_like_exception_line(line: &str) -> bool {
    (line.contains("Exception") || line.contains("Error"))
        && line.contains(':')
        && !line.starts_with('\t')
        && !line.starts_with(' ')
}

fn parse_crash_report(content: &str) -> ParsedCrash {
    let mut parsed = ParsedCrash::default();
    for line in content.lines() {
        if let Some(rest) = line.strip_prefix("Description: ") {
            parsed.description = Some(rest.trim().to_string());
        }
        if let Some(rest) = line.strip_prefix("\tScreen name: ") {
            parsed.screen = Some(rest.trim().to_string());
        }
        if parsed.exception_type.is_none() && looks_like_exception_line(line) {
            if let Some((ty, msg)) = line.split_once(':') {
                if ty.contains('.') || ty.ends_with("Exception") || ty.ends_with("Error") {
                    parsed.exception_type = Some(ty.trim().to_string());
                    parsed.exception_message = Some(msg.trim().to_string());
                }
            }
        }
        // at knot//pkg.Class(File.java:123)
        if let Some(rest) = line.trim().strip_prefix("at knot//") {
            if let Some((class_part, loc)) = rest.split_once('(') {
                let class_name = class_part.trim().to_string();
                let line_num = loc
                    .trim_end_matches(')')
                    .rsplit_once(':')
                    .and_then(|(_, n)| n.parse().ok());
                if class_name.contains("primeclient") || class_name.contains("dev.primeclient") {
                    parsed.prime_frames.push((class_name.clone(), line_num));
                }
                if let Some(idx) = class_name.find("dev.") {
                    let after = &class_name[idx + 4..];
                    if let Some(end) = after.find('.') {
                        let mod_id = &after[..end];
                        if !mod_id.is_empty() && !mod_id.starts_with("primeclient") {
                            parsed.mod_ids.push(mod_id.to_string());
                        }
                    }
                }
            }
        }
    }
    parsed.mod_ids.sort();
    parsed.mod_ids.dedup();
    parsed
}

fn suggest_fix(parsed: &ParsedCrash) -> &'static str {
    let ty = parsed
        .exception_type
        .as_deref()
        .unwrap_or("")
        .to_lowercase();
    let msg = parsed
        .exception_message
        .as_deref()
        .unwrap_or("")
        .to_lowercase();
    if msg.contains("can only blur once per frame") {
        return "blurOnce";
    }
    if ty.contains("outofmemoryerror") || msg.contains("java heap space") {
        return "outOfMemory";
    }
    if !parsed.prime_frames.is_empty() {
        return "primeMod";
    }
    if !parsed.mod_ids.is_empty() {
        return "modConflict";
    }
    if ty.contains("classnotfoundexception") || ty.contains("nosuchmethoderror") {
        return "modConflict";
    }
    if ty.contains("linkageerror") || msg.contains("fabric") {
        return "loaderError";
    }
    "unknown"
}

fn build_title(parsed: &ParsedCrash, fallback: &str) -> String {
    if let (Some(ty), Some(msg)) = (&parsed.exception_type, &parsed.exception_message) {
        return format!("{ty}: {msg}");
    }
    if let Some(ty) = &parsed.exception_type {
        return ty.clone();
    }
    if let Some(desc) = &parsed.description {
        return desc.clone();
    }
    fallback.into()
}

fn to_crash_dto(
    parsed: &ParsedCrash,
    source: &str,
    exit_code: Option<i32>,
    signal: Option<&str>,
    crash_report_path: Option<&str>,
    session_duration_sec: u64,
) -> Value {
    let prime_location = parsed.prime_frames.first().map(|(c, line)| {
        if let Some(l) = line {
            format!("{c}:{l}")
        } else {
            c.clone()
        }
    });
    json!({
        "source": source,
        "exitCode": exit_code,
        "signal": signal,
        "crashReportPath": crash_report_path,
        "title": build_title(parsed, "Minecraft crashed"),
        "description": parsed.description,
        "exceptionType": parsed.exception_type,
        "exceptionMessage": parsed.exception_message,
        "screen": parsed.screen,
        "primeInvolved": !parsed.prime_frames.is_empty(),
        "primeLocation": prime_location,
        "modIds": parsed.mod_ids.iter().take(5).cloned().collect::<Vec<_>>(),
        "fixKey": suggest_fix(parsed),
        "sessionDurationSec": session_duration_sec
    })
}

fn now_ms() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis())
        .unwrap_or(0)
}

pub fn analyze_game_exit(
    game_dir: &Path,
    known: &HashSet<String>,
    session_started_ms: u128,
    exit_code: Option<i32>,
    signal: Option<&str>,
    intentional_kill: bool,
    recent_log_lines: &[String],
) -> Result<Value, AppError> {
    let session_duration_sec = if session_started_ms > 0 {
        ((now_ms().saturating_sub(session_started_ms)) / 1000) as u64
    } else {
        0
    };

    if intentional_kill {
        return Ok(json!({
            "kind": "exit",
            "exit": {
                "reason": "launcher_kill",
                "exitCode": exit_code,
                "signal": signal,
                "sessionDurationSec": session_duration_sec
            }
        }));
    }

    if let Some(path) = find_new_crash_report(game_dir, known, session_started_ms) {
        if let Ok(content) = fs::read_to_string(&path) {
            let parsed = parse_crash_report(&content);
            return Ok(json!({
                "kind": "crash",
                "crash": to_crash_dto(
                    &parsed,
                    "crash_report",
                    exit_code,
                    signal,
                    Some(&path.to_string_lossy()),
                    session_duration_sec
                )
            }));
        }
    }

    let latest_log = game_dir.join("logs").join("latest.log");
    if let Ok(content) = fs::read_to_string(&latest_log) {
        if content.contains("---- Minecraft Crash Report ----") {
            if let Some(marker) = content.rfind("---- Minecraft Crash Report ----") {
                let parsed = parse_crash_report(&content[marker..]);
                return Ok(json!({
                    "kind": "crash",
                    "crash": to_crash_dto(
                        &parsed,
                        "latest_log",
                        exit_code,
                        signal,
                        None,
                        session_duration_sec
                    )
                }));
            }
        }
    }

    let start = recent_log_lines.len().saturating_sub(120);
    let tail = recent_log_lines[start..].join("\n");
    if tail.contains("---- Minecraft Crash Report ----")
        || tail.contains("FATAL ERROR in native method")
        || tail.contains("Process crashed with exit code")
    {
        let parsed = parse_crash_report(&tail);
        return Ok(json!({
            "kind": "crash",
            "crash": to_crash_dto(
                &parsed,
                "launch_log",
                exit_code,
                signal,
                None,
                session_duration_sec
            )
        }));
    }

    let abnormal = signal.is_some() || exit_code.map(|c| c != 0).unwrap_or(false);
    if abnormal {
        let parsed = ParsedCrash::default();
        return Ok(json!({
            "kind": "crash",
            "crash": to_crash_dto(
                &parsed,
                "exit_code",
                exit_code,
                signal,
                None,
                session_duration_sec
            )
        }));
    }

    Ok(json!({
        "kind": "exit",
        "exit": {
            "reason": "clean_quit",
            "exitCode": exit_code,
            "signal": signal,
            "sessionDurationSec": session_duration_sec
        }
    }))
}

#!/usr/bin/env python3
import argparse
import json
import os


def default_input_path():
    appdata = os.environ.get("APPDATA")
    if not appdata:
        appdata = os.path.join(os.path.expanduser("~"), "AppData", "Roaming")
    return os.path.join(
        appdata,
        "minecraft",
        "config",
        "HashimotoAddons",
        "prefix_token_candidates.json",
    )


def default_state_path(input_path):
    parent = os.path.dirname(os.path.abspath(input_path))
    return os.path.join(parent, "prefix_token_review_state.json")


def default_export_path(input_path):
    parent = os.path.dirname(os.path.abspath(input_path))
    return os.path.join(parent, "allowed_prefix_tokens.json")


def load_json(path):
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def save_json(path, payload):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def load_candidates(path):
    payload = load_json(path)
    candidates = payload.get("candidates", [])
    result = {}
    for entry in candidates:
        token = (entry.get("token") or "").strip()
        if not token:
            continue
        result[token] = {
            "count": int(entry.get("count") or 0),
            "examples": list(entry.get("examples") or []),
            "servers": list(entry.get("servers") or []),
            "positions": list(entry.get("positions") or []),
        }
    return result


def load_state(path):
    if not os.path.exists(path):
        return {"decisions": {}, "history": []}
    payload = load_json(path)
    decisions = payload.get("decisions", {})
    history = payload.get("history", [])
    if not isinstance(decisions, dict):
        decisions = {}
    if not isinstance(history, list):
        history = []
    return {"decisions": decisions, "history": history}


def ordered_tokens(candidates):
    return sorted(candidates.keys(), key=lambda token: (-candidates[token]["count"], token))


def build_export_payload(input_path, candidates, decisions):
    allowed = sorted(token for token, choice in decisions.items() if choice == "y")
    rejected = sorted(token for token, choice in decisions.items() if choice == "n")
    pending = sorted(token for token in candidates.keys() if token not in decisions)
    return {
        "sourceFile": os.path.abspath(input_path),
        "allowedPrefixTokens": allowed,
        "rejectedPrefixTokens": rejected,
        "pendingPrefixTokens": pending,
        "counts": {
            "allowed": len(allowed),
            "rejected": len(rejected),
            "pending": len(pending),
            "total": len(candidates),
        },
    }


def prompt_for_decision(token, info, progress_index, total_count):
    print()
    print(f"[{progress_index}/{total_count}] {token}")
    print(f"  occurrences: {info['count']}")
    positions = ", ".join(str(position) for position in sorted(set(info["positions"]))[:8])
    if positions:
        print(f"  positions: {positions}")
    examples = info["examples"][:5]
    if examples:
        print("  item examples:")
        for example in examples:
            print(f"    - {example}")
    servers = sorted(set(info["servers"]))[:3]
    if servers:
        print(f"  servers: {', '.join(servers)}")
    print("  commands: [y]es / [n]o / [b]ack / [q]uit")
    return input("> ").strip().lower()


def review(candidates, input_path, state_path, export_path):
    state = load_state(state_path)
    decisions = dict(state["decisions"])
    history = list(state["history"])
    tokens = ordered_tokens(candidates)

    def persist():
        save_json(state_path, {"decisions": decisions, "history": history})
        save_json(export_path, build_export_payload(input_path, candidates, decisions))

    persist()

    while True:
        pending = [token for token in tokens if token not in decisions]
        if not pending:
            print()
            print("All prefix tokens reviewed.")
            break

        current = pending[0]
        answer = prompt_for_decision(current, candidates[current], len(decisions) + 1, len(tokens))

        if answer in ("y", "yes"):
            decisions[current] = "y"
            history.append(current)
            persist()
            continue
        if answer in ("n", "no"):
            decisions[current] = "n"
            history.append(current)
            persist()
            continue
        if answer in ("b", "back"):
            if not history:
                print("Nothing to undo.")
                continue
            previous = history.pop()
            decisions.pop(previous, None)
            persist()
            print(f"Reverted previous choice: {previous}")
            continue
        if answer in ("q", "quit", "exit"):
            print("Saved progress and exiting.")
            return
        print("Unknown command. Use y, n, b, or q.")

    persist()
    print(f"State saved to:  {state_path}")
    print(f"Export saved to: {export_path}")


def main():
    parser = argparse.ArgumentParser(
        description="Review prefix token candidates extracted from HashimotoAddons item names.",
    )
    parser.add_argument(
        "input",
        nargs="?",
        default=default_input_path(),
        help="Path to prefix_token_candidates.json",
    )
    parser.add_argument("--state", default=None, help="Path to the review state JSON")
    parser.add_argument("--export", default=None, help="Path to the exported allowlist JSON")
    args = parser.parse_args()

    input_path = os.path.abspath(args.input)
    state_path = os.path.abspath(args.state or default_state_path(input_path))
    export_path = os.path.abspath(args.export or default_export_path(input_path))

    candidates = load_candidates(input_path)
    review(candidates, input_path, state_path, export_path)


if __name__ == "__main__":
    main()

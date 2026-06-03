#!/usr/bin/env python3
import argparse
import json
import os
import sys
from collections import defaultdict


def default_input_path():
    appdata = os.environ.get("APPDATA")
    if not appdata:
        return os.path.join(
            os.path.expanduser("~"),
            "AppData",
            "Roaming",
            "minecraft",
            "config",
            "HashimotoAddons",
            "evolution_forge_items.json",
        )
    return os.path.join(
        appdata,
        "minecraft",
        "config",
        "HashimotoAddons",
        "evolution_forge_items.json",
    )


def default_state_path(input_path):
    parent = os.path.dirname(os.path.abspath(input_path))
    return os.path.join(parent, "stat_name_review_state.json")


def default_export_path(input_path):
    parent = os.path.dirname(os.path.abspath(input_path))
    return os.path.join(parent, "stat_name_review_result.json")


def load_json(path):
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def save_json(path, payload):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def collect_stat_names(data):
    stats = defaultdict(lambda: {"count": 0, "items": set(), "servers": set(), "sources": set()})
    for server in data.get("servers", []):
        server_key = server.get("serverKey", "")
        for item_group in server.get("statRanges", []):
            item_name = item_group.get("itemName", "")
            for entry in item_group.get("ranges", []):
                stat_name = entry.get("statName", "")
                if not stat_name:
                    continue
                stats[stat_name]["count"] += 1
                if item_name:
                    stats[stat_name]["items"].add(item_name)
                if server_key:
                    stats[stat_name]["servers"].add(server_key)
                stats[stat_name]["sources"].add("statRanges")
        for item_group in server.get("observedBounds", []):
            item_name = item_group.get("itemName", "")
            for entry in item_group.get("bounds", []):
                stat_name = entry.get("statName", "")
                if not stat_name:
                    continue
                stats[stat_name]["count"] += 1
                if item_name:
                    stats[stat_name]["items"].add(item_name)
                if server_key:
                    stats[stat_name]["servers"].add(server_key)
                stats[stat_name]["sources"].add("observedBounds")
    return stats


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


def build_export_payload(input_path, stats, decisions):
    allowed = sorted(name for name, choice in decisions.items() if choice == "y")
    rejected = sorted(name for name, choice in decisions.items() if choice == "n")
    pending = sorted(name for name in stats.keys() if name not in decisions)
    return {
        "sourceFile": os.path.abspath(input_path),
        "allowedStatNames": allowed,
        "rejectedStatNames": rejected,
        "pendingStatNames": pending,
        "counts": {
            "allowed": len(allowed),
            "rejected": len(rejected),
            "pending": len(pending),
            "total": len(stats),
        },
    }


def ordered_names(stats):
    return sorted(
        stats.keys(),
        key=lambda name: (-stats[name]["count"], name),
    )


def prompt_for_decision(name, info, progress_index, total_count):
    print()
    print(f"[{progress_index}/{total_count}] {name}")
    print(f"  occurrences: {info['count']}")
    print(f"  sources: {', '.join(sorted(info['sources']))}")
    item_examples = sorted(info["items"])[:5]
    if item_examples:
        print("  item examples:")
        for item in item_examples:
            print(f"    - {item}")
    server_examples = sorted(info["servers"])[:3]
    if server_examples:
        print(f"  servers: {', '.join(server_examples)}")
    print("  commands: [y]es / [n]o / [b]ack / [q]uit")
    return input("> ").strip().lower()


def review(stats, input_path, state_path, export_path):
    state = load_state(state_path)
    decisions = dict(state["decisions"])
    history = list(state["history"])
    names = ordered_names(stats)

    def persist():
        save_json(state_path, {"decisions": decisions, "history": history})
        save_json(export_path, build_export_payload(input_path, stats, decisions))

    persist()

    while True:
        pending = [name for name in names if name not in decisions]
        if not pending:
            print()
            print("All stat names reviewed.")
            break

        current = pending[0]
        progress_index = len(decisions) + 1
        total_count = len(names)
        answer = prompt_for_decision(current, stats[current], progress_index, total_count)

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


def print_summary(stats, decisions):
    allowed = sum(1 for choice in decisions.values() if choice == "y")
    rejected = sum(1 for choice in decisions.values() if choice == "n")
    pending = len(stats) - len(decisions)
    print(f"total stat names: {len(stats)}")
    print(f"allowed: {allowed}")
    print(f"rejected: {rejected}")
    print(f"pending: {pending}")


def main():
    parser = argparse.ArgumentParser(
        description="Review statName entries extracted from evolution_forge_items.json.",
    )
    parser.add_argument(
        "input",
        nargs="?",
        default=default_input_path(),
        help="Path to evolution_forge_items.json",
    )
    parser.add_argument(
        "--state",
        default=None,
        help="Path to the review state JSON",
    )
    parser.add_argument(
        "--export",
        default=None,
        help="Path to the exported allow/reject result JSON",
    )
    args = parser.parse_args()

    input_path = os.path.abspath(args.input)
    state_path = os.path.abspath(args.state or default_state_path(input_path))
    export_path = os.path.abspath(args.export or default_export_path(input_path))

    if not os.path.exists(input_path):
        print(f"Input file not found: {input_path}", file=sys.stderr)
        return 1

    data = load_json(input_path)
    stats = collect_stat_names(data)
    if not stats:
        print("No statName entries were found.")
        return 0

    state = load_state(state_path)
    print(f"Input:  {input_path}")
    print(f"State:  {state_path}")
    print(f"Export: {export_path}")
    print_summary(stats, state["decisions"])
    review(stats, input_path, state_path, export_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

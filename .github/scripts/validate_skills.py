from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
SKILLS_DIR = ROOT / "codex" / "skills"
NAME_PATTERN = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
REFERENCE_PATTERN = re.compile(r"`(references/[^`]+)`")


def parse_frontmatter(skill_file: Path) -> tuple[dict[str, str], str]:
    text = skill_file.read_text(encoding="utf-8")
    lines = text.splitlines()
    if len(lines) < 4 or lines[0] != "---":
        raise ValueError("missing opening YAML frontmatter delimiter")

    try:
        closing_index = lines.index("---", 1)
    except ValueError as error:
        raise ValueError("missing closing YAML frontmatter delimiter") from error

    metadata: dict[str, str] = {}
    for line in lines[1:closing_index]:
        if not line.strip():
            continue
        if ":" not in line:
            raise ValueError(f"invalid frontmatter line: {line}")
        key, value = line.split(":", 1)
        metadata[key.strip()] = value.strip()
    return metadata, text


def validate_skill(skill_dir: Path) -> list[str]:
    errors: list[str] = []
    skill_file = skill_dir / "SKILL.md"
    if not skill_file.is_file():
        return [f"{skill_dir}: missing SKILL.md"]

    try:
        metadata, text = parse_frontmatter(skill_file)
    except (OSError, UnicodeError, ValueError) as error:
        return [f"{skill_file}: {error}"]

    name = metadata.get("name", "")
    description = metadata.get("description", "")
    extra_keys = sorted(set(metadata) - {"name", "description"})

    if name != skill_dir.name:
        errors.append(f"{skill_file}: name must match directory '{skill_dir.name}'")
    if not NAME_PATTERN.fullmatch(name):
        errors.append(f"{skill_file}: invalid skill name '{name}'")
    if not description or description.startswith("["):
        errors.append(f"{skill_file}: description must be a completed string")
    if extra_keys:
        errors.append(f"{skill_file}: unsupported frontmatter keys: {', '.join(extra_keys)}")

    for relative_reference in REFERENCE_PATTERN.findall(text):
        reference = skill_dir / relative_reference
        if not reference.is_file():
            errors.append(f"{skill_file}: missing referenced file '{relative_reference}'")

    openai_yaml = skill_dir / "agents" / "openai.yaml"
    if not openai_yaml.is_file():
        errors.append(f"{skill_dir}: missing agents/openai.yaml")

    return errors


def main() -> int:
    if not SKILLS_DIR.is_dir():
        print(f"Missing skills directory: {SKILLS_DIR}", file=sys.stderr)
        return 1

    skill_dirs = sorted(path for path in SKILLS_DIR.iterdir() if path.is_dir())
    if not skill_dirs:
        print(f"No skills found under {SKILLS_DIR}", file=sys.stderr)
        return 1

    errors = [error for skill_dir in skill_dirs for error in validate_skill(skill_dir)]
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    print(f"Validated {len(skill_dirs)} skill(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

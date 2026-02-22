# Gemini CLI Project Guide

## 🚀 Project Context: Conductor
This project uses the **Conductor** methodology.
**ALL** project documentation, architectural guidelines, and workflows are located in the `conductor/` directory.

- **Product Vision:** `conductor/product.md`
- **Design & Coding Standards:** `conductor/product-guidelines.md`
- **Tech Stack & Tools:** `conductor/tech-stack.md`
- **Development Workflow:** `conductor/workflow.md`
- **Current Tasks:** `conductor/tracks.md`

**Primary Instruction:** Follow the workflow defined in `conductor/workflow.md`.

## ⚠️ Critical Overrides (Read Carefully)

### 1. Git Operations
- **NO AUTOMATED COMMITS:** You must **NEVER** execute `git commit`.
- **Protocol:** Stage changes (`git add`), propose a commit message, and **ASK THE USER** to commit.
- **Branches:** Do not create or switch branches unless explicitly instructed.

### 2. WSL & Docker
- **Prefix Rule:** All `docker` commands must be prefixed with `wsl` (e.g., `wsl docker ps`).
- **Curl:** Execute `curl` natively (no `wsl` prefix).

### 3. Quality Gates
- **Strict Enforcement:** The build fails if Checkstyle or SpotBugs report violations.
- **Verification:** Always run `mvn clean verify` before asking for a commit.

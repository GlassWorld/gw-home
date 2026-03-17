# TASK ROUTER

## Classification

### SIMPLE
Single file change. No cross-domain impact.

Examples:
- Add a field to a DTO
- Fix a typo in a query
- Add a single Mapper method + XML

Action: Implement directly. No analysis needed.

---

### NORMAL
Multiple files in one domain. No external dependency change.

Examples:
- Add a new API endpoint (Controller + Service + Mapper + XML)
- Create a new domain structure
- Add pagination to an existing query

Action:
1. Read relevant domain files
2. Implement following skill template
3. Verify rule compliance (`CORE_RULES.md`)

---

### HEAVY
Cross-domain changes, schema changes, or architectural impact.

Examples:
- New domain from scratch
- Schema migration affecting multiple tables
- Shared module changes in `share`
- Auth/security changes

Action:
1. **WARN**: Output affected scope before starting
2. List all files that will be created or modified
3. Confirm domain boundaries are respected
4. Implement in order: DDL → Mapper → Service → Controller
5. Verify rule compliance after each layer

---

## Routing Checklist

- [ ] Does this change a DB schema? → HEAVY
- [ ] Does this affect more than one domain? → HEAVY
- [ ] Does this change `share` module? → HEAVY
- [ ] Does this add one endpoint in one domain? → NORMAL
- [ ] Does this change a single file? → SIMPLE

# Tuto 1.0.7 (Java 8 / 1.16.5)

**What's fixed**
- Config updates now merge into your existing `plugins/Tuto/config.yml` on reload/start (defaults copy).  
- `/tuto reload` persists in‑memory triggers, merges defaults, reloads `data.yml`, and re‑loads triggers.

**Build**
```bash
mvn -q -DskipTests clean package
# target/Tuto-1.0.7.jar
```

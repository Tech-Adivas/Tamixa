# Guardrails service: separate repository & submodule

The **structured guardrails HTTP API** lives in its **own Git repository** (`guardrails-service`) so it can be reused across products.

## Repository layout

| Path | Role |
|------|------|
| **`../guardrails-service`** (sibling of `araro-kids`) | Canonical standalone repo (Python/FastAPI). |
| **`araro-kids/guardrails-service`** | Git **submodule** (see `.gitmodules`). |

### Submodule URL on your machine

This monorepo may use a **`file://`** URL in `.gitmodules` so the submodule resolves to your **local** `guardrails-service` clone without needing the GitHub repo to exist yet. That path is **machine-specific**; teammates should either use the published **HTTPS** URL or clone `guardrails-service` beside `araro-kids` and run:

```bash
git config submodule.guardrails-service.url file:///absolute/path/to/guardrails-service
git submodule sync
```

After you publish the guardrails repo, commit `.gitmodules` with the **HTTPS** URL and run:

```bash
git submodule sync
```

## Clone Tamixa monorepo with submodule

```bash
git clone --recurse-submodules <your-araro-kids-url>
# or after clone:
git submodule update --init --recursive
```

If submodule URL fails (e.g. you have not published `guardrails-service` yet), clone both repos as **siblings** and set the URL:

```bash
git clone <araro-kids-url> araro-kids
git clone <guardrails-service-url> guardrails-service
cd araro-kids
# Point submodule at sibling (or edit .gitmodules to https and sync)
git config submodule.guardrails-service.url ../guardrails-service
git submodule sync
git submodule update --init
```

## Publish `guardrails-service` to GitHub

1. Create an empty repository, e.g. `github.com/<ORG>/guardrails-service`.
2. From your local `guardrails-service` folder:

   ```bash
   cd /path/to/guardrails-service
   git remote add origin git@github.com:<ORG>/guardrails-service.git
   git push -u origin main
   ```

3. In **this** monorepo, update `.gitmodules`:

   ```ini
   [submodule "guardrails-service"]
       path = guardrails-service
       url = https://github.com/<ORG>/guardrails-service.git
   ```

   Then:

   ```bash
   git submodule sync
   ```

## Docker Compose

`docker-compose.yml` builds `./guardrails-service`. After `git submodule update --init`, that directory is populated and the build works.

## See also

- [guardrails-service/README.md](../guardrails-service/README.md) (in submodule checkout)
- [AI_GUARDRAILS.md](AI_GUARDRAILS.md)

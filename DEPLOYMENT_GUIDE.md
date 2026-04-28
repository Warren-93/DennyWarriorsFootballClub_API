# Denny Warriors FC — Hosting & Deployment Guide

A practical, step-by-step deployment plan for the Denny Warriors FC stack:

- **Frontend** — React 18 (Create React App) — `DennyWarriors_Website`
- **API** — Spring Boot 3.2.5 / Java 17 — `DennyWarriorsAPI`
- **Database** — MongoDB

---

## 1. Recommended Stack at a Glance

| Layer | Recommendation | Why | Cost |
|---|---|---|---|
| React frontend | **Vercel** (primary) or **Render Static Site** | Zero-config CRA build, instant global CDN, auto-deploy from GitHub, free SSL/custom domains | Free tier is plenty for a club site |
| Spring Boot API | **Render — Web Service** | First-class Java support, builds straight from `pom.xml`, free tier available, you already use Render | Free tier (sleeps when idle) or $7/mo for always-on |
| MongoDB | **MongoDB Atlas — M0 Free Cluster** | Official managed MongoDB, 512 MB free, no card required, works with Spring Data Mongo out of the box | Free (M0) |

### Strong alternatives

- **Frontend** — Netlify or Cloudflare Pages (both excellent free tiers)
- **API** — Railway (~$5/mo, no cold starts), Fly.io (free allowance), Google Cloud Run (pay-per-request)
- **DB** — Atlas is essentially the only sensible choice for managed MongoDB; if you wanted self-hosted NoSQL you could use Render's managed databases or DigitalOcean

### A note on "free" Spring Boot hosting

Render's free tier spins the API down after ~15 min of inactivity, so the first request after idle takes 30–60 seconds to respond ("cold start"). For a club website, this is usually acceptable. If you want zero cold starts, upgrade to Render's Starter plan ($7/mo) or use Railway.

---

## 2. Step 1 — MongoDB Atlas (set up first; everything else needs the connection string)

1. Go to [https://www.mongodb.com/cloud/atlas/register](https://www.mongodb.com/cloud/atlas/register) and sign up.
2. **Create a project** (e.g. *Denny Warriors FC*).
3. **Build a cluster** → choose the free **M0** tier.
   - Provider: **AWS**
   - Region: pick the one closest to your users (e.g. *eu-west-2 London* for UK).
4. **Database Access** → *Add New Database User*.
   - Authentication: *Password*.
   - Username: `dwfc_api`, generate a strong password — **save it**.
   - Privileges: *Read and write to any database*.
5. **Network Access** → *Add IP Address*.
   - For initial testing: `0.0.0.0/0` (allow from anywhere).
   - For production: add Render's outbound IPs (Render → service → *Connect* → *Outbound IPs*) and remove the `0.0.0.0/0` rule.
6. **Connect** → *Drivers* → copy the connection string. It looks like:
   ```
   mongodb+srv://dwfc_api:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```
   Replace `<password>` with the real password and add the database name before the `?`:
   ```
   mongodb+srv://dwfc_api:REALPASSWORD@cluster0.xxxxx.mongodb.net/dennywarriors?retryWrites=true&w=majority
   ```
   **Save this string** — you'll paste it into Render as an env var.

---

## 3. Step 2 — Spring Boot API on Render

### 3a. Prepare the project

Create `src/main/resources/application.properties` (or use `application.yml`) so the app reads config from environment variables:

```properties
spring.application.name=DennyWarriorsAPI
server.port=${PORT:8080}

# MongoDB
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=dennywarriors

# CORS — allow your Vercel/Render frontend domain
app.cors.allowed-origins=${ALLOWED_ORIGINS:http://localhost:3000}

# Mail (if you keep spring-boot-starter-mail)
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Actuator — expose health for Render's healthcheck
management.endpoints.web.exposure.include=health,info
```

Make sure your CORS configuration (in your `WebSecurity`/`@Configuration` class) reads from `app.cors.allowed-origins`. If you don't have one yet, add a `WebMvcConfigurer` that wires the property into `CorsRegistry`.

### 3b. Push to GitHub

```bash
cd D:\DennyWarriors\DennyWarriorsAPI
git init
git add .
git commit -m "Initial commit"
# create a new private repo on github.com, then:
git remote add origin https://github.com/<your-user>/DennyWarriorsAPI.git
git branch -M main
git push -u origin main
```

(Skip if it's already on GitHub.)

### 3c. Create the Render service

1. [https://dashboard.render.com](https://dashboard.render.com) → **New** → **Web Service**.
2. Connect your GitHub account, pick the `DennyWarriorsAPI` repo.
3. Configure:
   - **Name:** `dennywarriors-api`
   - **Region:** match your Atlas region (e.g. Frankfurt or Ohio).
   - **Branch:** `main`
   - **Runtime:** *Docker* is not needed — choose **Java**. (If Render asks, set Java version to **17**.)
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/DennyWarriorsAPI-0.0.1-SNAPSHOT.jar`
   - **Instance Type:** Free (or Starter for $7/mo no cold starts).
4. **Environment Variables** (click *Advanced*):
   - `MONGODB_URI` = your Atlas connection string from Step 2.
   - `ALLOWED_ORIGINS` = `https://your-frontend.vercel.app` (you'll update this after deploying the frontend).
   - `MAIL_USERNAME`, `MAIL_PASSWORD` etc. if you use mail.
5. **Health Check Path:** `/actuator/health`
6. Click **Create Web Service**. First build takes ~5 minutes.
7. Once green, note the URL: `https://dennywarriors-api.onrender.com` — this is your API base URL.

### 3d. Verify

```
https://dennywarriors-api.onrender.com/actuator/health
```
should return `{"status":"UP"}`.

---

## 4. Step 3 — React Frontend on Vercel

### 4a. Prepare the project

Add `D:\Users\mrren\DennyWarriors_Website\.env.production`:

```
REACT_APP_API_BASE_URL=https://dennywarriors-api.onrender.com
```

In your code, switch hard-coded API URLs (or the `axios.create` baseURL) to:

```js
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || "http://localhost:8080",
});
```

Update `package.json` — remove the `homepage` field (Vercel serves from root):

```json
// delete this line:
"homepage": "https://dennywarriorsfootballclub-website.onrender.com/DennyWarriorsFootballClub_Website/",
```

Add `D:\Users\mrren\DennyWarriors_Website\vercel.json` so React Router deep links work:

```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

Make sure `node_modules/` is in `.gitignore` before committing.

### 4b. Push to GitHub

```bash
cd D:\Users\mrren\DennyWarriors_Website
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/<your-user>/DennyWarriors_Website.git
git branch -M main
git push -u origin main
```

### 4c. Deploy on Vercel

1. [https://vercel.com](https://vercel.com) → sign in with GitHub.
2. **Add New** → **Project** → import `DennyWarriors_Website`.
3. Vercel auto-detects Create React App — accept the defaults:
   - Build Command: `npm run build`
   - Output Directory: `build`
4. **Environment Variables:** add `REACT_APP_API_BASE_URL` = `https://dennywarriors-api.onrender.com`.
5. **Deploy.** ~2 minutes. You'll get a URL like `https://dennywarriors-website.vercel.app`.

### 4d. Wire CORS back to the API

Go back to Render → API service → *Environment* → update `ALLOWED_ORIGINS`:

```
ALLOWED_ORIGINS=https://dennywarriors-website.vercel.app
```

Render will redeploy automatically. The frontend should now talk to the API.

---

## 5. Step 4 — Custom Domain (optional)

If you have a domain (say `dennywarriorsfc.com`):

1. **Frontend** — Vercel → project → *Domains* → add `dennywarriorsfc.com` and `www.dennywarriorsfc.com`. Vercel shows you the DNS records to add at your registrar (A/CNAME). SSL is automatic.
2. **API** — Render → service → *Settings* → *Custom Domain* → add `api.dennywarriorsfc.com`. Add the CNAME at your registrar pointing to the Render URL.
3. Update `ALLOWED_ORIGINS` on Render and `REACT_APP_API_BASE_URL` on Vercel to use the new domains.

---

## 6. Going-Live Checklist

- [ ] Atlas Network Access locked down to Render outbound IPs (remove `0.0.0.0/0`).
- [ ] All secrets (DB URI, mail password) live as env vars, not in source control.
- [ ] `application.properties` checked in **without** real credentials.
- [ ] Spring Security rules reviewed — admin endpoints behind auth, public endpoints open.
- [ ] CORS allows only your real frontend origin(s).
- [ ] `/actuator/health` returns 200 → Render keeps the service marked healthy.
- [ ] Atlas backups: M0 doesn't include them — for a small site, set a weekly `mongodump` reminder, or upgrade to M2/M5 ($9+/mo) for continuous backups.
- [ ] Vercel and Render both auto-deploy on `git push` to `main` — confirm by pushing a small change.

---

## 7. Costs Summary

| Service | Free tier | Paid tier (recommended) |
|---|---|---|
| Vercel | Unlimited static bandwidth, generous build minutes | $20/mo Pro (only if you need teams) |
| Render Web Service | 750 hrs/mo, sleeps after 15 min idle | $7/mo Starter — no sleep, 512 MB RAM |
| MongoDB Atlas | M0 cluster — 512 MB | M2 — $9/mo (2 GB + backups) |

**Realistic monthly bill for a club site that you want always-on:** ~$7 (Render Starter), everything else stays free. Total free-tier setup is also viable if you accept ~30s cold starts.

---

## 8. Quick Reference — Where Things Live After Setup

| Asset | URL |
|---|---|
| Frontend | `https://dennywarriors-website.vercel.app` |
| API | `https://dennywarriors-api.onrender.com` |
| API health | `https://dennywarriors-api.onrender.com/actuator/health` |
| Database | MongoDB Atlas dashboard → cluster `Cluster0` |
| Frontend deploys | GitHub `main` branch → Vercel |
| API deploys | GitHub `main` branch → Render |

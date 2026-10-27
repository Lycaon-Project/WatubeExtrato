# Security policy / Politique de sécurité

## Reporting a vulnerability / Signaler une vulnérabilité

Please report vulnerabilities **privately** through
[GitHub's private vulnerability reporting](https://github.com/Lycaon-Project/WatubeExtrato/security/advisories/new),
not in a public issue. Include the affected version (commit hash), the steps to reproduce and
the impact you observed.

🇫🇷 Merci de signaler les vulnérabilités **en privé** via le
[signalement privé de GitHub](https://github.com/Lycaon-Project/WatubeExtrato/security/advisories/new),
et non dans une issue publique, avec le commit concerné, les étapes pour reproduire et
l'impact constaté.

## Supported versions / Versions maintenues

Only the latest commit of the `watube-Extracto` branch, the one used by Watube, receives fixes.

🇫🇷 Seul le dernier commit de la branche `watube-Extracto`, utilisé par Watube, reçoit des
correctifs.

## Scope / Périmètre

In scope / Dans le périmètre:

- execution of code coming from the network outside of the JavaScript sandbox;
- a response making the library fetch, or hand to the player, a URL outside of the allowed
  hosts (YouTube's sites and video servers);
- leaks of user identifying data in requests (beyond the IP address, inherent to any request);
- crashes or unbounded resource use triggered by a response;
- vulnerable dependencies.

Out of scope / Hors périmètre: YouTube changes breaking extraction (please open a normal issue),
and anything that requires a modified copy of the library.

## Hardening in place / Durcissement en place

| Area | Measure |
|---|---|
| JavaScript from YouTube | Rhino sandbox: interpreted mode, 5 s run time limit, bounded stack, Java classes refused |
| Player JavaScript origin | `https://www.youtube.com` only, final URL after redirects checked |
| Streaming URLs | `https://*.googlevideo.com` only (formats, HLS, SABR, DASH and its redirects) |
| Links | YouTube redirects unwrapped, tracking parameters removed |
| Identifiers | `SecureRandom` nonces, fresh visitor data per player request, no PoToken |
| Concurrency | thread safe, bounded caches |
| Supply chain | actions pinned by commit hash, Gradle wrapper checksum, Dependabot, OSV checks |

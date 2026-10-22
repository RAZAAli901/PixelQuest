# Certificate pinning

S038. The app refuses a TLS connection to `*.supabase.co` unless the server's certificate chain includes one of the pinned public keys (`app/src/main/res/xml/network_security_config.xml`; debug builds pin the same in `src/debug`). A certificate wrongly issued by any other CA, even one the phone trusts, is refused, and so is a CA installed on the phone.

## What's pinned

Android's format: the SHA-256 of a certificate's SubjectPublicKeyInfo, base64.

| CA | Key | Why |
|---|---|---|
| Google Trust Services Root R4 | `mEflZT5e…` | Serves the project today: chain `supabase.co` ← WE1 ← GTS Root R4 |
| GTS intermediate WE1 | `kIdp6NNE…` | Today's intermediate |
| GTS Root R1, R2, R3 | `hxqRlPTu…`, `Vfd95BwD…`, `QXnt2YHv…` | GTS's other roots |
| ISRG Root X1, X2 (Let's Encrypt) | `C5+lpZ7t…`, `diGVwiVY…` | Cloudflare also issues from Let's Encrypt |
| SSL.com RSA, ECC, TLS ECC 2022 | `0cRTd+vc…`, `oyD01TTX…`, `G/ANXI8T…` | And from SSL.com |
| Sectigo R46, E46 | `Douxi77v…`, `sLVjNUaF…` | And from Sectigo |

Roots last until 2035 to 2046, and Cloudflare can move between these CAs without the app noticing. **The pins expire on 2027-06-30.** After that date Android stops enforcing them, and the system's normal checks still apply, so an outdated app is never locked out.

## Checking the live chain (every few months, and before the expiry)

```bash
H=<project ref>.supabase.co
echo | openssl s_client -connect $H:443 -servername $H -showcerts 2>/dev/null | grep -E " [0-9] s:| i:"
```

Each certificate's key hash:
```bash
echo | openssl s_client -connect $H:443 -servername $H -showcerts 2>/dev/null \
  | awk '/BEGIN CERT/{n++} n==2' | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
```

For roots, run the scratch `Pins.java` (S038's commit) against a JDK's `lib/security/cacerts`. It prints each root's hash. On Windows the shell's line endings break the openssl pipeline above; Java avoids that.

## Rotating

1. If the chain's root or intermediate isn't in the list, add its hash. Keep the old ones until every supported app version has the new list.
2. Move `expiration` about a year past the release date.
3. Release. Old versions keep working until their own expiry, then fall back to normal checks.

## If pinning breaks connections (an incident)

Android reports a pinning failure as an SSL error, and the app shows its "can't reach the server" state. The fix is a release with the new pin. Until then, players on a version past its expiry date are unaffected. See [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md).

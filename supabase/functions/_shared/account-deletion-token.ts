const TOKEN_BYTES = 32;

export function createAccountDeletionToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(TOKEN_BYTES));
  const token = base64UrlEncode(bytes);
  return { token, tokenHashPromise: hashToken(token) };
}

export async function hashToken(token: string) {
  const hashBuffer = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return base64UrlEncode(new Uint8Array(hashBuffer));
}

function base64UrlEncode(bytes: Uint8Array) {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

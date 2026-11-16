
export async function onRequest({ request, env }) {
  if (!env.API_ORIGIN) {
    return new Response("API_ORIGIN is not configured", {
      status: 500
    });
  }

  const incomingUrl = new URL(request.url);
  const backendUrl = new URL(env.API_ORIGIN);

  backendUrl.pathname = incomingUrl.pathname;
  backendUrl.search = incomingUrl.search;

  // Forward the request without the browser's Origin header.
  const originalRequest = new Request(backendUrl.toString(), request);
  const headers = new Headers(originalRequest.headers);
  headers.delete("Origin");

  const proxyRequest = new Request(originalRequest, {
    headers
  });

  return fetch(proxyRequest);
}

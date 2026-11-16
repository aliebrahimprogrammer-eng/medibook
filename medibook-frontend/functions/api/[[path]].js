
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

  return fetch(new Request(backendUrl.toString(), request));
}

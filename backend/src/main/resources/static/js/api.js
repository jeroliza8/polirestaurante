import { state } from './state.js';

// Ruta relativa: funciona igual en http://servidor:8080/polirestaurante/ que en la raiz.
const BASE = 'api';

let onExpired = () => {};
export const onSessionExpired = cb => { onExpired = cb; };

async function request(path, { method = 'GET', body, headers = {} } = {}) {
  const opts = { method, headers: { ...headers } };
  if (state.token) opts.headers.Authorization = 'Bearer ' + state.token;
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  let r;
  try {
    r = await fetch(BASE + path, opts);
  } catch {
    throw Object.assign(new Error('No hay conexión con el servidor. Revise la red e intente de nuevo.'), { status: 0 });
  }
  if (r.status === 401 && path !== '/auth/login') {
    onExpired();
    throw Object.assign(new Error('Su sesión expiró. Inicie sesión de nuevo.'), { status: 401 });
  }
  return r;
}

/** Llama a la API y devuelve el JSON. Si falla lanza Error con el mensaje del backend ({error}). */
export async function api(path, opts) {
  const r = await request(path, opts);
  if (r.status === 204) return null;
  const data = await r.json().catch(() => ({}));
  if (!r.ok) {
    throw Object.assign(new Error(data.error || `Error ${r.status}`), { status: r.status, data });
  }
  return data;
}

export const get = path => api(path);
export const post = (path, body = {}) => api(path, { method: 'POST', body });
export const put = (path, body = {}) => api(path, { method: 'PUT', body });
export const patch = (path, body = {}) => api(path, { method: 'PATCH', body });
export const del = path => api(path, { method: 'DELETE' });

/** Descarga un archivo (PDF / Excel) respetando el token. */
export async function descargar(path, nombre) {
  const r = await request(path);
  if (!r.ok) {
    const data = await r.json().catch(() => ({}));
    throw new Error(data.error || `Error ${r.status}`);
  }
  const blob = await r.blob();
  const url = URL.createObjectURL(blob);
  const a = Object.assign(document.createElement('a'), { href: url, download: nombre });
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

/** Construye ?a=1&b=2 omitiendo vacios. */
export function qs(params) {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== null && v !== '') p.append(k, v);
  const s = p.toString();
  return s ? '?' + s : '';
}

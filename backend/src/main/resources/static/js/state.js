// Sesion del usuario. Se guarda en sessionStorage: se borra al cerrar el navegador.
const KEY = 'polirestaurante.sesion';

function leer() {
  try { return JSON.parse(sessionStorage.getItem(KEY)) || {}; } catch { return {}; }
}

export const state = { ...leer() };

export function setSession({ token, rol, nombre, usuarioId, expiraEnSegundos }) {
  Object.assign(state, { token, rol, nombre, usuarioId, expiraEn: Date.now() + expiraEnSegundos * 1000 });
  try { sessionStorage.setItem(KEY, JSON.stringify(state)); } catch { /* almacenamiento no disponible */ }
}

export function clearSession() {
  for (const k of Object.keys(state)) delete state[k];
  try { sessionStorage.removeItem(KEY); } catch { /* nada */ }
}

export function sesionVigente() {
  return Boolean(state.token) && state.expiraEn > Date.now();
}

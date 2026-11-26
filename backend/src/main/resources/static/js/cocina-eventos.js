import { state, sesionVigente } from './state.js';

/** Canal común del Observer. El JWT se transmite en un frame, no como parámetro URL. */
export function conectarEventosCocina({ alEvento = () => {}, alEstado = () => {} } = {}) {
  let ws, detenido = false, temporizador, revision, enviado, espera = 1000;
  const url = new URL('ws/cocina', location.href);
  url.protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  function autenticar() {
    if (ws?.readyState === WebSocket.OPEN && sesionVigente() && state.token !== enviado) {
      ws.send(JSON.stringify({ tipo: 'AUTENTICAR', token: state.token }));
      enviado = state.token;
    }
  }
  function iniciar() {
    if (detenido || !sesionVigente()) return;
    alEstado('conectando');
    const socket = new WebSocket(url);
    ws = socket;
    enviado = null;
    socket.onopen = () => { if (socket === ws) autenticar(); };
    socket.onmessage = e => {
      if (socket !== ws || detenido) return;
      try {
        const dato = JSON.parse(e.data);
        if (dato.tipo === 'CONECTADO') { espera = 1000; alEstado('conectado'); }
        alEvento(dato);
      } catch (error) { console.error('Mensaje de cocina no procesado', error); }
    };
    socket.onerror = () => { if (socket === ws) alEstado('desconectado'); };
    socket.onclose = () => {
      if (socket !== ws || detenido) return;
      alEstado('desconectado');
      if (sesionVigente()) {
        temporizador = setTimeout(iniciar, espera);
        espera = Math.min(espera * 2, 10000);
      }
    };
  }
  function cerrar() {
    detenido = true;
    clearTimeout(temporizador);
    clearInterval(revision);
    if (ws && ws.readyState < WebSocket.CLOSING) ws.close();
  }
  revision = setInterval(() => { if (sesionVigente()) autenticar(); else cerrar(); }, 5000);
  window.addEventListener('pagehide', cerrar, { once: true });
  iniciar();
  return cerrar;
}

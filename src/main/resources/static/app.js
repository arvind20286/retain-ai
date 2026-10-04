const wsUrl = (() => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/review`;
})();

const chatBox = document.getElementById('chatBox');
const statusEl = document.getElementById('status');
const statusLine = document.getElementById('statusLine');
const connectBtn = document.getElementById('connectBtn');
const micBtn = document.getElementById('micBtn');
const sendBtn = document.getElementById('sendBtn');
const textInput = document.getElementById('textInput');

let socket = null;
let micActive = false;
let audioContext = null;
let mediaStream = null;
let scriptNode = null;
let nextStartTime = 0;
let scheduledSources = [];

function addMessage(text, kind = 'bot') {
  const el = document.createElement('div');
  el.className = `message ${kind}`;
  el.textContent = text;
  chatBox.appendChild(el);
  chatBox.scrollTop = chatBox.scrollHeight;
}

function setStatus(kind, text) {
  statusEl.className = `status ${kind}`;
  statusEl.textContent = text;
}

function ensureAudioContext() {
  if (!audioContext) {
    audioContext = new (window.AudioContext || window.webkitAudioContext)();
  }
  if (audioContext.state === 'suspended') {
    audioContext.resume();
  }
  return audioContext;
}

function int16ToFloat32(int16Array) {
  const float32 = new Float32Array(int16Array.length);
  for (let i = 0; i < int16Array.length; i++) {
    float32[i] = int16Array[i] / 32768;
  }
  return float32;
}

function float32ToInt16(float32Array) {
  const int16 = new Int16Array(float32Array.length);
  for (let i = 0; i < float32Array.length; i++) {
    const s = Math.max(-1, Math.min(1, float32Array[i]));
    int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
  }
  return int16;
}

function downsampleBuffer(buffer, inputRate, outputRate) {
  if (inputRate === outputRate) return buffer;

  const ratio = inputRate / outputRate;
  const outputLength = Math.floor(buffer.length / ratio);
  const output = new Float32Array(outputLength);

  for (let i = 0; i < outputLength; i++) {
    const position = i * ratio;
    const left = Math.floor(position);
    const fraction = position - left;
    const right = Math.min(left + 1, buffer.length - 1);
    output[i] = buffer[left] * (1 - fraction) + buffer[right] * fraction;
  }

  return output;
}

function playPcmAudio(buffer, sampleRate = 24000) {
  try {
    const ctx = ensureAudioContext();
    const bytes = new Uint8Array(buffer);
    const int16 = new Int16Array(bytes.buffer, bytes.byteOffset, Math.floor(bytes.byteLength / 2));
    const float32 = int16ToFloat32(int16);
    const audioBuffer = ctx.createBuffer(1, float32.length, sampleRate);
    audioBuffer.getChannelData(0).set(float32);

    const source = ctx.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(ctx.destination);

    const now = ctx.currentTime;
    nextStartTime = Math.max(now, nextStartTime);
    source.start(nextStartTime);
    nextStartTime += audioBuffer.duration;

    scheduledSources.push(source);
    source.onended = () => {
      const idx = scheduledSources.indexOf(source);
      if (idx > -1) scheduledSources.splice(idx, 1);
    };
  } catch (error) {
    console.error('Failed to play audio response:', error);
  }
}

function connect() {
  setStatus('connecting', 'Connecting');
  statusLine.textContent = 'Opening live session...';
  socket = new WebSocket(wsUrl);
  socket.binaryType = 'arraybuffer';

  socket.onopen = () => {
    setStatus('connected', 'Connected');
    statusLine.textContent = 'Live session ready';
    addMessage('Connected. You can type or use your microphone.', 'bot');
  };

  socket.onmessage = (event) => {
    try {
      if (event.data instanceof ArrayBuffer) {
        playPcmAudio(event.data, 24000);
        return;
      }

      if (event.data instanceof Blob) {
        event.data.arrayBuffer().then((buf) => playPcmAudio(buf, 24000));
        return;
      }

      const msg = JSON.parse(event.data);
      if (msg.type === 'gemini' && msg.text) {
        addMessage(msg.text, 'bot');
      } else if (msg.type === 'turn_complete') {
        statusLine.textContent = 'Turn complete';
      } else if (msg.type === 'interaction_status') {
        statusLine.textContent = 'Interaction ready';
      } else if (msg.error) {
        addMessage(msg.error, 'bot');
      }
    } catch (e) {
      console.log('Non-JSON message', event.data);
    }
  };

  socket.onclose = () => {
    setStatus('disconnected', 'Disconnected');
    statusLine.textContent = 'Session closed';
  };

  socket.onerror = () => {
    setStatus('error', 'Error');
    statusLine.textContent = 'Connection error';
  };
}

function sendText() {
  const text = textInput.value.trim();
  if (!text) return;
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ text }));
    addMessage(text, 'user');
    textInput.value = '';
  }
}

async function startMic() {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    addMessage('Connect first before using mic.', 'bot');
    return;
  }

  if (micActive) {
    stopMic();
    return;
  }

  try {
    const ctx = ensureAudioContext();
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const source = ctx.createMediaStreamSource(mediaStream);
    scriptNode = ctx.createScriptProcessor(4096, 1, 1);

    scriptNode.onaudioprocess = (event) => {
      const input = event.inputBuffer.getChannelData(0);
      const mono16k = downsampleBuffer(input, ctx.sampleRate, 16000);
      const int16 = float32ToInt16(mono16k);
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(int16.buffer);
      }
    };

    source.connect(scriptNode);
    const mutedOutput = ctx.createGain();
    mutedOutput.gain.value = 0;
    scriptNode.connect(mutedOutput);
    mutedOutput.connect(ctx.destination);
    micActive = true;
    micBtn.textContent = 'Stop Mic';
    statusLine.textContent = 'Mic live';
  } catch (e) {
    console.error(e);
    addMessage('Microphone access failed. Please allow browser microphone permission.', 'bot');
  }
}

function stopMic() {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ audioStreamEnd: true }));
  }
  micActive = false;
  micBtn.textContent = 'Start Mic';
  statusLine.textContent = 'Mic off • Chat ready';
  if (scriptNode) {
    scriptNode.disconnect();
    scriptNode = null;
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach((t) => t.stop());
    mediaStream = null;
  }
}

function stopAudioPlayback() {
  scheduledSources.forEach((source) => {
    try {
      source.stop();
    } catch (e) {
      // ignore
    }
  });
  scheduledSources = [];
  if (audioContext) {
    nextStartTime = audioContext.currentTime;
  }
}

connectBtn.addEventListener('click', () => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.close();
  }
  connect();
});

micBtn.addEventListener('click', startMic);
sendBtn.addEventListener('click', sendText);
textInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') sendText();
});

addMessage('Click Connect to start the Gemini Live session.', 'bot');

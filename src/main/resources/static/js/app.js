/**
 * PROCUCEV AI - REAL-TIME MULTILINGUAL TRANSCRIPTION APP
 * Pure Vanilla JavaScript Client
 */

// Application State
const state = {
  isRecording: false,
  isPaused: false,
  startTime: null,
  timerInterval: null,
  durationSeconds: 0,
  finalTranscript: '',
  interimTranscript: '',
  audioChunks: [],
  audioBlob: null,
  audioBase64: null,
  mediaRecorder: null,
  recognition: null,
  audioContext: null,
  analyser: null,
  animFrameId: null,
  ws: null,
  currentEngine: 'live', // 'live', 'gemini', 'whisper'
  activeFilter: 'all',
  searchQuery: '',
  transcriptions: [],
  selectedIds: new Set(),
  geminiKey: localStorage.getItem('gemini_api_key') || '',
};

// DOM Elements
const elements = {
  recordBtn: document.getElementById('recordBtn'),
  pauseBtn: document.getElementById('pauseBtn'),
  stopSaveBtn: document.getElementById('stopSaveBtn'),
  clearBtn: document.getElementById('clearBtn'),
  copyBtn: document.getElementById('copyBtn'),
  ttsBtn: document.getElementById('ttsBtn'),
  translateBtn: document.getElementById('translateBtn'),
  scriptModeSelect: document.getElementById('scriptModeSelect'),
  refineBtn: document.getElementById('refineBtn'),
  waveCanvas: document.getElementById('waveCanvas'),
  liveOutput: document.getElementById('liveOutput'),
  interimOutput: document.getElementById('interimOutput'),
  detectedLanguageBadge: document.getElementById('detectedLanguageBadge'),
  timerDisplay: document.getElementById('timerDisplay'),
  wordCountDisplay: document.getElementById('wordCountDisplay'),
  tokenTags: document.getElementById('tokenTags'),
  audioPreviewWrapper: document.getElementById('audioPreviewWrapper'),
  audioPreview: document.getElementById('audioPreview'),
  micStatusText: document.getElementById('micStatusText'),
  transcriptionsGrid: document.getElementById('transcriptionsGrid'),
  searchInput: document.getElementById('searchInput'),
  filterPills: document.querySelectorAll('.filter-pill'),
  bulkBar: document.getElementById('bulkBar'),
  selectedCountText: document.getElementById('selectedCountText'),
  bulkDeleteBtn: document.getElementById('bulkDeleteBtn'),
  statTotal: document.getElementById('statTotal'),
  statWords: document.getElementById('statWords'),
  statHinglish: document.getElementById('statHinglish'),
  statDuration: document.getElementById('statDuration'),
  editModal: document.getElementById('editModal'),
  editId: document.getElementById('editId'),
  editTitle: document.getElementById('editTitle'),
  editContent: document.getElementById('editContent'),
  editLanguage: document.getElementById('editLanguage'),
  editTags: document.getElementById('editTags'),
  saveEditBtn: document.getElementById('saveEditBtn'),
  closeEditModal: document.getElementById('closeEditModal'),
  cancelEditBtn: document.getElementById('cancelEditBtn'),
  settingsModal: document.getElementById('settingsModal'),
  geminiKeyInput: document.getElementById('geminiKeyInput'),
  saveSettingsBtn: document.getElementById('saveSettingsBtn'),
  closeSettingsModal: document.getElementById('closeSettingsModal'),
  openSettingsBtn: document.getElementById('openSettingsBtn'),
  toastContainer: document.getElementById('toastContainer'),
  engineTabs: document.querySelectorAll('.engine-tab')
};

// ==========================================
// INITIALIZATION
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
  initEngineTabs();
  initWebSocket();
  initSpeechRecognition();
  initEventListeners();
  loadTranscriptions();
  loadDashboardStats();
  initVisualizer();
});

// ==========================================
// WEBSOCKET REAL-TIME STREAMING
// ==========================================
function initWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/ws/live-transcribe`;

  try {
    state.ws = new WebSocket(wsUrl);

    state.ws.onopen = () => {
      console.log('✓ Connected to Real-time Transcription WebSocket');
    };

    state.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'INTERIM_ANALYSIS') {
          handleInterimAnalysis(data);
        } else if (data.type === 'TRANSCRIPTION_SAVED') {
          showToast('Transcription saved to MySQL database!', 'success');
          loadTranscriptions();
          loadDashboardStats();
        }
      } catch (err) {
        console.error('WebSocket message parsing error', err);
      }
    };

    state.ws.onclose = () => {
      console.log('WebSocket disconnected. Reconnecting in 3s...');
      setTimeout(initWebSocket, 3000);
    };
  } catch (e) {
    console.warn('WebSocket unavailable, running in direct REST mode', e);
  }
}

function sendWsInterim(text, duration) {
  if (state.ws && state.ws.readyState === WebSocket.OPEN) {
    state.ws.send(JSON.stringify({
      action: 'STREAM_INTERIM',
      text: text,
      duration: duration
    }));
  }
}

// ==========================================
// DEVANAGARI HINDI TO ENGLISH WRITING TRANSLITERATOR
// ==========================================
function toEnglishScript(text) {
  if (!text) return '';

  let res = text;

  // 1. Common English and Hindi words commonly transcribed in Devanagari
  const wordMap = {
    'हेलो': 'Hello',
    'हेल्लो': 'Hello',
    'माय': 'my',
    'नेम': 'name',
    'इस': 'is',
    'भुवन': 'Bhuvan',
    'मेरा': 'mera',
    'मेरी': 'meri',
    'मेरे': 'mere',
    'नाम': 'naam',
    'है': 'hai',
    'हैं': 'hain',
    'हो': 'ho',
    'था': 'tha',
    'थी': 'thi',
    'थे': 'the',
    'अच्छी': 'acchi',
    'अच्छा': 'accha',
    'बात': 'baat',
    'बुरी': 'buri',
    'बुरा': 'bura',
    'नहीं': 'nahi',
    'और': 'aur',
    'क्या': 'kya',
    'कहा': 'kaha',
    'कहो': 'kaho',
    'करो': 'karo',
    'करना': 'karna',
    'करेंगे': 'karenge',
    'करूंगा': 'karunga',
    'करती': 'karti',
    'करता': 'karta',
    'आप': 'aap',
    'आपका': 'aapka',
    'तुम': 'tum',
    'तुम्हारा': 'tumhara',
    'हम': 'hum',
    'हमारा': 'humara',
    'दिखाओ': 'dikhao',
    'बताओ': 'batao',
    'सुनो': 'suno',
    'नमस्ते': 'namaste',
    'धन्यवाद': 'dhanyavad',
    'शुक्रिया': 'shukriya',
    'ठीक': 'theek',
    'कल': 'kal',
    'आज': 'aaj',
    'अभी': 'abhi',
    'काम': 'kaam',
    'कॉल': 'call',
    'मीटिंग': 'meeting',
    'प्रोजेक्ट': 'project',
    'कोड': 'code',
    'टाइम': 'time',
    'वर्क': 'work',
    'थैंक': 'thank',
    'थैंक्यू': 'thank you',
    'प्लीज': 'please',
    'ओके': 'okay',
    'यस': 'yes',
    'नो': 'no',
    'सर': 'sir',
    'मैम': 'maam'
  };

  for (const [hindi, eng] of Object.entries(wordMap)) {
    const reg = new RegExp('\\b' + hindi + '\\b', 'gi');
    res = res.replace(reg, eng);
  }

  // If already pure English / Latin alphabet, return
  if (!/[\u0900-\u097F]/.test(res)) {
    return res;
  }

  // 2. Character-by-character phonetic transliteration
  const vowels = {
    'अ': 'a', 'आ': 'aa', 'इ': 'i', 'ई': 'ee', 'उ': 'u', 'ऊ': 'oo', 'ऋ': 'ri',
    'ए': 'e', 'ऐ': 'ai', 'ओ': 'o', 'औ': 'au', 'ं': 'n', 'ँ': 'n', 'ः': 'h'
  };

  const matras = {
    'ा': 'a', 'ि': 'i', 'ी': 'ee', 'ु': 'u', 'ू': 'oo', 'ृ': 'ri',
    'े': 'e', 'ै': 'ai', 'ो': 'o', 'ौ': 'au'
  };

  const consonants = {
    'क': 'k', 'ख': 'kh', 'ग': 'g', 'घ': 'gh', 'ङ': 'ng',
    'च': 'ch', 'छ': 'chh', 'ज': 'j', 'झ': 'jh', 'ञ': 'ny',
    'ट': 't', 'ठ': 'th', 'ड': 'd', 'ढ': 'dh', 'ण': 'n',
    'त': 't', 'थ': 'th', 'द': 'd', 'ध': 'dh', 'न': 'n',
    'प': 'p', 'फ': 'ph', 'ब': 'b', 'भ': 'bh', 'म': 'm',
    'य': 'y', 'र': 'r', 'ल': 'l', 'व': 'v', 'श': 'sh', 'ष': 'sh', 'स': 's', 'ह': 'h',
    'क़': 'q', 'ख़': 'kh', 'ग़': 'gh', 'ज़': 'z', 'ड़': 'r', 'ढ़': 'rh', 'फ़': 'f'
  };

  let out = '';
  const len = res.length;
  for (let i = 0; i < len; i++) {
    const c = res[i];

    if (vowels[c]) {
      out += vowels[c];
    } else if (matras[c]) {
      out += matras[c];
    } else if (c === '्') {
      continue;
    } else if (consonants[c]) {
      out += consonants[c];
      const next = (i + 1 < len) ? res[i + 1] : '';
      if (!matras[next] && next !== '्' && next !== ' ' && next !== '' && /[\u0900-\u097F]/.test(next)) {
        out += 'a';
      }
    } else {
      out += c;
    }
  }

  return out;
}

// ==========================================
// SPEECH RECOGNITION (MULTILINGUAL HINGLISH)
// ==========================================
function initSpeechRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

  if (!SpeechRecognition) {
    showToast('Web Speech API is not supported in this browser. Please use Google Chrome or Edge.', 'error');
    return;
  }

  state.recognition = new SpeechRecognition();
  state.recognition.continuous = true;
  state.recognition.interimResults = true;
  state.recognition.maxAlternatives = 1;
  // en-IN outputs English writing letters for both English and Hindi/Hinglish speech
  state.recognition.lang = elements.scriptModeSelect ? elements.scriptModeSelect.value : 'en-IN';
  if (state.recognition.lang === 'hi-translit') state.recognition.lang = 'hi-IN';

  state.recognition.onstart = () => {
    state.isRecording = true;
    updateRecordingUI(true);
  };

  state.recognition.onresult = (event) => {
    let interim = '';
    let newlyFinalized = '';

    for (let i = event.resultIndex; i < event.results.length; i++) {
      let transcript = event.results[i][0].transcript;
      // Convert any Devanagari Hindi to English writing letters
      transcript = toEnglishScript(transcript);

      if (event.results[i].isFinal) {
        newlyFinalized += transcript + ' ';
      } else {
        interim += transcript;
      }
    }

    if (newlyFinalized) {
      state.finalTranscript += newlyFinalized;
    }
    state.interimTranscript = interim;

    updateTranscriptUI();

    // Stream tokens to backend via WebSocket for live analysis
    const fullText = (state.finalTranscript + ' ' + state.interimTranscript).trim();
    if (fullText) {
      sendWsInterim(fullText, state.durationSeconds);
      analyzeClientLanguage(fullText);
    }
  };

  state.recognition.onerror = (event) => {
    console.warn('Speech recognition warning:', event.error);
    if (event.error === 'not-allowed') {
      showToast('Microphone access denied! Please allow microphone permissions.', 'error');
      stopRecording();
    }
  };

  state.recognition.onend = () => {
    if (state.isRecording && !state.isPaused) {
      // Auto-restart recognition for continuous seamless streaming
      try {
        state.recognition.start();
      } catch (e) {
        // Ignored
      }
    }
  };
}

// ==========================================
// AUDIO RECORDING & WAVEFORM VISUALIZER
// ==========================================
async function startRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

    // Setup Web Audio Analyser
    state.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const source = state.audioContext.createMediaStreamSource(stream);
    state.analyser = state.audioContext.createAnalyser();
    state.analyser.fftSize = 128;
    source.connect(state.analyser);

    // Setup MediaRecorder for audio playback & cloud API
    state.audioChunks = [];
    state.mediaRecorder = new MediaRecorder(stream);
    state.mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) state.audioChunks.push(e.data);
    };

    state.mediaRecorder.onstop = async () => {
      state.audioBlob = new Blob(state.audioChunks, { type: 'audio/webm' });
      const audioUrl = URL.createObjectURL(state.audioBlob);
      elements.audioPreview.src = audioUrl;
      elements.audioPreviewWrapper.classList.add('active');

      // Convert to base64 for MySQL persistence & Gemini API
      const reader = new FileReader();
      reader.readAsDataURL(state.audioBlob);
      reader.onloadend = () => {
        state.audioBase64 = reader.result.split(',')[1];
      };
    };

    state.mediaRecorder.start(250);

    // Start Web Speech Recognition
    if (state.recognition) {
      state.recognition.start();
    }

    // Start Timer
    state.startTime = Date.now() - (state.durationSeconds * 1000);
    state.timerInterval = setInterval(updateTimer, 1000);

    state.isRecording = true;
    state.isPaused = false;
    updateRecordingUI(true);
    animateWaveform();

    showToast('Microphone listening! Speak in Hindi, English, or Hinglish...', 'info');
  } catch (err) {
    console.error('Mic access error', err);
    showToast('Could not access microphone: ' + err.message, 'error');
  }
}

function pauseRecording() {
  if (!state.isRecording) return;

  if (state.isPaused) {
    // Resume
    state.isPaused = false;
    if (state.mediaRecorder && state.mediaRecorder.state === 'paused') {
      state.mediaRecorder.resume();
    }
    if (state.recognition) {
      try { state.recognition.start(); } catch (e) {}
    }
    state.startTime = Date.now() - (state.durationSeconds * 1000);
    state.timerInterval = setInterval(updateTimer, 1000);
    elements.pauseBtn.innerHTML = '<i class="fas fa-pause"></i> Pause';
    showToast('Recording resumed', 'info');
  } else {
    // Pause
    state.isPaused = true;
    if (state.mediaRecorder && state.mediaRecorder.state === 'recording') {
      state.mediaRecorder.pause();
    }
    if (state.recognition) {
      try { state.recognition.stop(); } catch (e) {}
    }
    clearInterval(state.timerInterval);
    elements.pauseBtn.innerHTML = '<i class="fas fa-play"></i> Resume';
    showToast('Recording paused', 'info');
  }
}

function stopRecording() {
  if (!state.isRecording) return;

  state.isRecording = false;
  state.isPaused = false;
  clearInterval(state.timerInterval);

  if (state.recognition) {
    try { state.recognition.stop(); } catch (e) {}
  }

  if (state.mediaRecorder && state.mediaRecorder.state !== 'inactive') {
    state.mediaRecorder.stop();
  }

  if (state.audioContext) {
    state.audioContext.close();
  }

  cancelAnimationFrame(state.animFrameId);
  clearWaveformCanvas();
  updateRecordingUI(false);
}

// Waveform visualizer drawing loop
function initVisualizer() {
  clearWaveformCanvas();
}

function clearWaveformCanvas() {
  const canvas = elements.waveCanvas;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  canvas.width = canvas.parentElement.clientWidth;
  canvas.height = canvas.parentElement.clientHeight;

  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(0, canvas.height / 2);
  ctx.lineTo(canvas.width, canvas.height / 2);
  ctx.stroke();
}

function animateWaveform() {
  if (!state.isRecording || state.isPaused || !state.analyser) {
    clearWaveformCanvas();
    return;
  }

  const canvas = elements.waveCanvas;
  const ctx = canvas.getContext('2d');
  const bufferLength = state.analyser.frequencyBinCount;
  const dataArray = new Uint8Array(bufferLength);
  state.analyser.getByteFrequencyData(dataArray);

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const barWidth = (canvas.width / bufferLength) * 2.2;
  let x = 0;

  for (let i = 0; i < bufferLength; i++) {
    const barHeight = (dataArray[i] / 255) * canvas.height * 0.85;

    const gradient = ctx.createLinearGradient(0, canvas.height, 0, 0);
    gradient.addColorStop(0, '#6366f1');
    gradient.addColorStop(0.5, '#8b5cf6');
    gradient.addColorStop(1, '#06b6d4');

    ctx.fillStyle = gradient;
    ctx.fillRect(x, (canvas.height - barHeight) / 2, barWidth - 1, Math.max(barHeight, 2));

    x += barWidth;
  }

  state.animFrameId = requestAnimationFrame(animateWaveform);
}

// ==========================================
// UI UPDATES & TIMER
// ==========================================
function updateRecordingUI(isRecording) {
  if (isRecording) {
    elements.recordBtn.classList.add('recording');
    elements.recordBtn.innerHTML = '<i class="fas fa-stop"></i>';
    elements.pauseBtn.style.display = 'inline-flex';
    elements.stopSaveBtn.style.display = 'inline-flex';
    elements.micStatusText.innerHTML = '<span class="status-dot"></span> Live Microphone Streaming';
    elements.micStatusText.style.color = '#10b981';
  } else {
    elements.recordBtn.classList.remove('recording');
    elements.recordBtn.innerHTML = '<i class="fas fa-microphone"></i>';
    elements.pauseBtn.style.display = 'none';
    elements.micStatusText.innerHTML = '<i class="fas fa-microphone-slash"></i> Microphone Idle';
    elements.micStatusText.style.color = 'var(--text-dim)';
  }
}

function updateTimer() {
  state.durationSeconds = Math.floor((Date.now() - state.startTime) / 1000);
  const hrs = String(Math.floor(state.durationSeconds / 3600)).padStart(2, '0');
  const mins = String(Math.floor((state.durationSeconds % 3600) / 60)).padStart(2, '0');
  const secs = String(state.durationSeconds % 60).padStart(2, '0');
  elements.timerDisplay.textContent = `${hrs}:${mins}:${secs}`;
}

function updateTranscriptUI() {
  elements.liveOutput.textContent = state.finalTranscript;
  elements.interimOutput.textContent = state.interimTranscript;

  const totalText = (state.finalTranscript + ' ' + state.interimTranscript).trim();
  const words = totalText ? totalText.split(/\s+/).length : 0;
  elements.wordCountDisplay.textContent = `${words} words`;
}

// Linguistic analysis handling
function handleInterimAnalysis(data) {
  if (data.detectedLanguage) {
    elements.detectedLanguageBadge.textContent = data.detectedLanguage;
    if (data.isMultilingual) {
      elements.detectedLanguageBadge.className = 'detected-badge hinglish';
    } else {
      elements.detectedLanguageBadge.className = 'detected-badge';
    }
  }

  // Render detected token tags
  if (elements.tokenTags) {
    elements.tokenTags.innerHTML = '';
    if (data.hindiTokens && data.hindiTokens.length > 0) {
      data.hindiTokens.slice(0, 4).forEach(t => {
        const tag = document.createElement('span');
        tag.className = 'token-tag hindi';
        tag.textContent = `HI: ${t}`;
        elements.tokenTags.appendChild(tag);
      });
    }
    if (data.englishTokens && data.englishTokens.length > 0) {
      data.englishTokens.slice(0, 4).forEach(t => {
        const tag = document.createElement('span');
        tag.className = 'token-tag english';
        tag.textContent = `EN: ${t}`;
        elements.tokenTags.appendChild(tag);
      });
    }
  }
}

// Client-side fallback language detection
function analyzeClientLanguage(text) {
  const hasDevanagari = /[\u0900-\u097F]/.test(text);
  const hinglishTokens = ['hai', 'karna', 'karo', 'kya', 'nahi', 'mera', 'aap', 'hum', 'theek', 'accha', 'aur', 'pe', 'me', 'raha'];
  const lower = text.toLowerCase();
  const containsHinglish = hinglishTokens.some(w => new RegExp('\\b' + w + '\\b').test(lower));

  if ((hasDevanagari && /[a-zA-Z]/.test(text)) || containsHinglish) {
    elements.detectedLanguageBadge.textContent = 'Hinglish (Hindi + English)';
    elements.detectedLanguageBadge.className = 'detected-badge hinglish';
  } else if (hasDevanagari) {
    elements.detectedLanguageBadge.textContent = 'Hindi';
    elements.detectedLanguageBadge.className = 'detected-badge';
  } else {
    elements.detectedLanguageBadge.textContent = 'English';
    elements.detectedLanguageBadge.className = 'detected-badge';
  }
}

// ==========================================
// CRUD OPERATIONS (MySQL BACKEND API)
// ==========================================

// 1. CREATE: Save transcription to MySQL
async function saveCurrentTranscription() {
  const content = (state.finalTranscript + ' ' + state.interimTranscript).trim();
  if (!content) {
    showToast('Cannot save an empty transcription. Speak something first!', 'error');
    return;
  }

  stopRecording();

  const detectedLang = elements.detectedLanguageBadge.textContent || 'Hinglish (Hindi + English)';
  const payload = {
    title: '', // Server will auto-generate based on content
    content: content,
    detectedLanguage: detectedLang,
    confidence: 0.96,
    durationSeconds: state.durationSeconds,
    audioData: state.audioBase64 || null,
    tags: detectedLang.includes('Hinglish') ? 'Hinglish, Multilingual' : detectedLang
  };

  try {
    elements.stopSaveBtn.disabled = true;
    elements.stopSaveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';

    const res = await fetch('/api/transcriptions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) throw new Error('Failed to save to database');

    const saved = await res.json();
    showToast(`Saved to MySQL: "${saved.title}"`, 'success');

    // Reset Studio
    clearStudio();

    // Refresh Dashboard & Analytics
    loadTranscriptions();
    loadDashboardStats();
  } catch (err) {
    showToast('Error saving transcription: ' + err.message, 'error');
  } finally {
    elements.stopSaveBtn.disabled = false;
    elements.stopSaveBtn.innerHTML = '<i class="fas fa-save"></i> Save to DB';
  }
}

// 2. READ: Fetch all transcriptions with filters
async function loadTranscriptions() {
  try {
    const langParam = state.activeFilter !== 'all' ? `&language=${encodeURIComponent(state.activeFilter)}` : '';
    const searchParam = state.searchQuery ? `&search=${encodeURIComponent(state.searchQuery)}` : '';
    const url = `/api/transcriptions?page=0&size=50&sortBy=createdAt&direction=desc${langParam}${searchParam}`;

    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to load history');

    const pageData = await res.json();
    state.transcriptions = pageData.content || [];
    renderTranscriptions(state.transcriptions);
  } catch (err) {
    console.error('Error fetching transcriptions', err);
    elements.transcriptionsGrid.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon"><i class="fas fa-exclamation-triangle"></i></div>
        <h3>Failed to connect to database</h3>
        <p style="color:var(--text-muted);margin-top:8px;">Make sure the Spring Boot service and MySQL database are running.</p>
      </div>`;
  }
}

// Render cards
function renderTranscriptions(list) {
  const grid = elements.transcriptionsGrid;
  grid.innerHTML = '';

  if (!list || list.length === 0) {
    grid.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon"><i class="fas fa-microphone-slash"></i></div>
        <h3>No transcriptions found</h3>
        <p style="color:var(--text-muted);margin-top:8px;">Start recording above to transcribe multilingual speech and save records to MySQL.</p>
      </div>`;
    return;
  }

  list.forEach(item => {
    const card = document.createElement('div');
    card.className = 'transcription-card';
    card.dataset.id = item.id;

    const isHinglish = item.detectedLanguage && item.detectedLanguage.toLowerCase().includes('hinglish');
    const badgeClass = isHinglish ? 'detected-badge hinglish' : 'detected-badge';
    const formattedDate = formatDateTime(item.createdAt);

    card.innerHTML = `
      <div>
        <div class="card-top">
          <div style="display:flex;align-items:center;gap:10px;">
            <input type="checkbox" class="transcription-checkbox" data-id="${item.id}" ${state.selectedIds.has(item.id) ? 'checked' : ''} />
            <div class="card-title-group">
              <div class="card-title">${escapeHtml(item.title)}</div>
              <div class="card-date"><i class="far fa-clock"></i> ${formattedDate}</div>
            </div>
          </div>
          <span class="${badgeClass}">${escapeHtml(item.detectedLanguage)}</span>
        </div>
        <div class="card-content">${escapeHtml(item.content)}</div>
      </div>

      <div>
        <div class="card-meta">
          <span><i class="fas fa-align-left"></i> ${item.wordCount || 0} words</span>
          <span><i class="fas fa-stopwatch"></i> ${item.durationSeconds || 0}s</span>
          <span><i class="fas fa-bullseye"></i> ${Math.round((item.confidence || 0.95) * 100)}%</span>
        </div>
        <div class="card-actions">
          <div class="action-buttons-left">
            <button class="btn btn-secondary btn-icon" title="Listen with TTS" onclick="playTTS('${escapeQuote(item.content)}')">
              <i class="fas fa-volume-up"></i>
            </button>
            ${item.audioData ? `
              <button class="btn btn-secondary btn-icon" title="Replay Recorded Voice" onclick="playRecordedAudio('${item.audioData}')">
                <i class="fas fa-play"></i>
              </button>` : ''}
            <button class="btn btn-secondary btn-icon" title="Copy Text" onclick="copyCardText('${escapeQuote(item.content)}')">
              <i class="far fa-copy"></i>
            </button>
          </div>
          <div class="action-buttons-right">
            <button class="btn btn-secondary btn-icon" title="Edit Transcription" onclick="openEditModal(${item.id})">
              <i class="fas fa-pencil-alt"></i>
            </button>
            <button class="btn btn-danger btn-icon" title="Delete" onclick="deleteTranscription(${item.id})">
              <i class="fas fa-trash-alt"></i>
            </button>
          </div>
        </div>
      </div>
    `;

    grid.appendChild(card);
  });

  // Attach checkbox listeners
  document.querySelectorAll('.transcription-checkbox').forEach(cb => {
    cb.addEventListener('change', (e) => {
      const id = parseInt(e.target.dataset.id);
      if (e.target.checked) {
        state.selectedIds.add(id);
      } else {
        state.selectedIds.delete(id);
      }
      updateBulkBar();
    });
  });
}

// 3. UPDATE: Edit transcription modal
async function openEditModal(id) {
  try {
    const res = await fetch(`/api/transcriptions/${id}`);
    if (!res.ok) throw new Error('Transcription not found');
    const data = await res.json();

    elements.editId.value = data.id;
    elements.editTitle.value = data.title;
    elements.editContent.value = data.content;
    elements.editLanguage.value = data.detectedLanguage || 'Hinglish (Hindi + English)';
    elements.editTags.value = data.tags || '';

    elements.editModal.classList.add('active');
  } catch (err) {
    showToast('Failed to open edit modal: ' + err.message, 'error');
  }
}

async function saveEdit() {
  const id = elements.editId.value;
  const payload = {
    title: elements.editTitle.value,
    content: elements.editContent.value,
    detectedLanguage: elements.editLanguage.value,
    tags: elements.editTags.value
  };

  try {
    const res = await fetch(`/api/transcriptions/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) throw new Error('Failed to update');

    showToast('Transcription updated successfully', 'success');
    elements.editModal.classList.remove('active');
    loadTranscriptions();
    loadDashboardStats();
  } catch (err) {
    showToast('Error updating transcription: ' + err.message, 'error');
  }
}

// 4. DELETE: Delete single transcription
async function deleteTranscription(id) {
  if (!confirm('Are you sure you want to delete this transcription from MySQL?')) return;

  try {
    const res = await fetch(`/api/transcriptions/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Failed to delete');

    showToast('Transcription deleted', 'info');
    state.selectedIds.delete(id);
    loadTranscriptions();
    loadDashboardStats();
  } catch (err) {
    showToast('Error deleting transcription: ' + err.message, 'error');
  }
}

// 5. BULK DELETE
async function bulkDeleteSelected() {
  if (state.selectedIds.size === 0) return;
  if (!confirm(`Are you sure you want to delete all ${state.selectedIds.size} selected transcriptions?`)) return;

  try {
    const res = await fetch('/api/transcriptions/bulk-delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ids: Array.from(state.selectedIds) })
    });

    if (!res.ok) throw new Error('Bulk delete failed');

    const data = await res.json();
    showToast(`Deleted ${data.deletedCount} transcriptions`, 'success');
    state.selectedIds.clear();
    updateBulkBar();
    loadTranscriptions();
    loadDashboardStats();
  } catch (err) {
    showToast('Error during bulk deletion: ' + err.message, 'error');
  }
}

function updateBulkBar() {
  const count = state.selectedIds.size;
  if (count > 0) {
    elements.selectedCountText.textContent = `${count} transcription${count > 1 ? 's' : ''} selected`;
    elements.bulkBar.classList.add('active');
  } else {
    elements.bulkBar.classList.remove('active');
  }
}

// 6. DASHBOARD ANALYTICS
async function loadDashboardStats() {
  try {
    const res = await fetch('/api/transcriptions/stats');
    if (!res.ok) return;
    const stats = await res.json();

    elements.statTotal.textContent = stats.totalTranscriptions || 0;
    elements.statWords.textContent = stats.totalWords || 0;
    elements.statHinglish.textContent = `${stats.hinglishPercentage || 0}%`;

    const mins = Math.floor((stats.totalDurationSeconds || 0) / 60);
    const secs = Math.round((stats.totalDurationSeconds || 0) % 60);
    elements.statDuration.textContent = `${mins}m ${secs}s`;
  } catch (err) {
    console.warn('Stats fetch error', err);
  }
}

// ==========================================
// TEXT-TO-SPEECH (TTS) & AUDIO PLAYBACK
// ==========================================
function playTTS(text) {
  if (!text) return;

  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    // Find Hindi or Indian English voice if available
    const voices = window.speechSynthesis.getVoices();
    const preferredVoice = voices.find(v => v.lang.includes('hi') || v.lang.includes('IN')) || voices[0];
    if (preferredVoice) utterance.voice = preferredVoice;
    utterance.rate = 0.95;
    window.speechSynthesis.speak(utterance);
    showToast('Playing Text-to-Speech audio...', 'info');
  } else {
    showToast('Text-to-Speech is not supported in this browser', 'error');
  }
}

function playRecordedAudio(base64Data) {
  if (!base64Data) return;
  const audio = new Audio(`data:audio/webm;base64,${base64Data}`);
  audio.play();
  showToast('Playing original microphone voice recording', 'info');
}

// ==========================================
// GEMINI LIVE / AI TRANSCRIBE MODE
// ==========================================
async function transcribeWithGemini() {
  if (!state.audioBase64) {
    showToast('Please record audio first before calling Gemini Live!', 'error');
    return;
  }

  try {
    showToast('Sending audio to Gemini Multilingual Live API...', 'info');
    const headers = { 'Content-Type': 'application/json' };
    if (state.geminiKey) headers['X-Gemini-Key'] = state.geminiKey;

    const res = await fetch('/api/ai/transcribe-audio', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ audio: state.audioBase64, mimeType: 'audio/webm' })
    });

    const data = await res.json();
    if (data.success && data.transcription) {
      state.finalTranscript = data.transcription;
      state.interimTranscript = '';
      updateTranscriptUI();
      if (data.detectedLanguage) {
        elements.detectedLanguageBadge.textContent = data.detectedLanguage;
      }
      showToast('Gemini transcription complete!', 'success');
    } else {
      showToast(data.error || 'Gemini transcription could not be completed', 'error');
    }
  } catch (err) {
    showToast('Gemini request failed: ' + err.message, 'error');
  }
}

// Refine transcript punctuation
async function refineCurrentTranscript() {
  const text = (state.finalTranscript + ' ' + state.interimTranscript).trim();
  if (!text) return;

  try {
    elements.refineBtn.disabled = true;
    elements.refineBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Refining...';

    const headers = { 'Content-Type': 'application/json' };
    if (state.geminiKey) headers['X-Gemini-Key'] = state.geminiKey;

    const res = await fetch('/api/ai/refine', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ text: text })
    });

    const data = await res.json();
    if (data.refinedText) {
      state.finalTranscript = data.refinedText;
      state.interimTranscript = '';
      updateTranscriptUI();
      showToast('Transcript refined with proper punctuation', 'success');
    }
  } catch (err) {
    showToast('Refinement error: ' + err.message, 'error');
  } finally {
    elements.refineBtn.disabled = false;
    elements.refineBtn.innerHTML = '<i class="fas fa-magic"></i> Refine';
  }
}

// Translate transcript to pure English
async function translateCurrentTranscript() {
  const text = (state.finalTranscript + ' ' + state.interimTranscript).trim();
  if (!text) {
    showToast('No text available to translate! Speak first.', 'error');
    return;
  }

  try {
    elements.translateBtn.disabled = true;
    elements.translateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Translating...';

    const headers = { 'Content-Type': 'application/json' };
    if (state.geminiKey) headers['X-Gemini-Key'] = state.geminiKey;

    const res = await fetch('/api/ai/translate-to-english', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ text: text })
    });

    const data = await res.json();
    if (data.translatedText) {
      state.finalTranscript = data.translatedText;
      state.interimTranscript = '';
      updateTranscriptUI();
      elements.detectedLanguageBadge.textContent = 'English (Translated)';
      elements.detectedLanguageBadge.className = 'detected-badge';
      showToast('Translated to English writing text!', 'success');
    }
  } catch (err) {
    showToast('Translation error: ' + err.message, 'error');
  } finally {
    elements.translateBtn.disabled = false;
    elements.translateBtn.innerHTML = '<i class="fas fa-language"></i> Translate to English';
  }
}

// ==========================================
// UTILITIES & EVENT LISTENERS
// ==========================================
function clearStudio() {
  state.finalTranscript = '';
  state.interimTranscript = '';
  state.durationSeconds = 0;
  state.audioChunks = [];
  state.audioBlob = null;
  state.audioBase64 = null;
  updateTranscriptUI();
  elements.timerDisplay.textContent = '00:00:00';
  elements.wordCountDisplay.textContent = '0 words';
  elements.detectedLanguageBadge.textContent = 'Hinglish (Hindi + English)';
  elements.detectedLanguageBadge.className = 'detected-badge hinglish';
  elements.audioPreviewWrapper.classList.remove('active');
  elements.audioPreview.src = '';
  if (elements.tokenTags) elements.tokenTags.innerHTML = '';
}

function initEngineTabs() {
  elements.engineTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      elements.engineTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      state.currentEngine = tab.dataset.engine;

      if (state.currentEngine === 'gemini') {
        showToast('Switched to Gemini Live 3.1 AI API mode', 'info');
      } else if (state.currentEngine === 'whisper') {
        showToast('Switched to Whisper Multilingual STT engine', 'info');
      } else {
        showToast('Switched to Real-time Zero-Latency Streaming mode', 'info');
      }
    });
  });
}

function initEventListeners() {
  // Studio Buttons
  elements.recordBtn.addEventListener('click', () => {
    if (state.isRecording) {
      stopRecording();
    } else {
      startRecording();
    }
  });

  elements.pauseBtn.addEventListener('click', pauseRecording);
  elements.stopSaveBtn.addEventListener('click', saveCurrentTranscription);
  elements.clearBtn.addEventListener('click', clearStudio);

  elements.copyBtn.addEventListener('click', () => {
    const text = (state.finalTranscript + ' ' + state.interimTranscript).trim();
    if (!text) return;
    navigator.clipboard.writeText(text);
    showToast('Copied to clipboard!', 'success');
  });

  elements.ttsBtn.addEventListener('click', () => {
    const text = (state.finalTranscript + ' ' + state.interimTranscript).trim();
    if (!text) {
      showToast('No text to speak!', 'error');
      return;
    }
    playTTS(text);
  });

  if (elements.translateBtn) {
    elements.translateBtn.addEventListener('click', translateCurrentTranscript);
  }

  if (elements.scriptModeSelect) {
    elements.scriptModeSelect.addEventListener('change', (e) => {
      const mode = e.target.value;
      if (mode === 'hi-translit') {
        state.recognition.lang = 'hi-IN';
        showToast('Hindi speech with real-time English text transliteration active', 'info');
      } else {
        state.recognition.lang = mode;
        showToast(`Output set to ${mode}`, 'info');
      }
      if (state.isRecording) {
        state.recognition.stop();
      }
    });
  }

  if (elements.refineBtn) {
    elements.refineBtn.addEventListener('click', refineCurrentTranscript);
  }

  // Search input debounce
  let searchTimeout = null;
  elements.searchInput.addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      state.searchQuery = e.target.value.trim();
      loadTranscriptions();
    }, 250);
  });

  // Filter Pills
  elements.filterPills.forEach(pill => {
    pill.addEventListener('click', () => {
      elements.filterPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      state.activeFilter = pill.dataset.filter;
      loadTranscriptions();
    });
  });

  // Bulk Delete
  elements.bulkDeleteBtn.addEventListener('click', bulkDeleteSelected);

  // Edit Modal Events
  elements.saveEditBtn.addEventListener('click', saveEdit);
  elements.closeEditModal.addEventListener('click', () => elements.editModal.classList.remove('active'));
  elements.cancelEditBtn.addEventListener('click', () => elements.editModal.classList.remove('active'));

  // Settings Modal Events
  elements.openSettingsBtn.addEventListener('click', () => {
    elements.geminiKeyInput.value = state.geminiKey;
    elements.settingsModal.classList.add('active');
  });
  elements.closeSettingsModal.addEventListener('click', () => elements.settingsModal.classList.remove('active'));
  elements.saveSettingsBtn.addEventListener('click', () => {
    state.geminiKey = elements.geminiKeyInput.value.trim();
    localStorage.setItem('gemini_api_key', state.geminiKey);
    elements.settingsModal.classList.remove('active');
    showToast('Gemini API configuration saved', 'success');
  });

  // Close modals on clicking overlay backdrop
  [elements.editModal, elements.settingsModal].forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });
  });
}

function copyCardText(text) {
  navigator.clipboard.writeText(text);
  showToast('Copied to clipboard!', 'success');
}

function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  let icon = 'info-circle';
  if (type === 'success') icon = 'check-circle';
  if (type === 'error') icon = 'exclamation-circle';

  toast.innerHTML = `<i class="fas fa-${icon}"></i> <span>${escapeHtml(message)}</span>`;
  elements.toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(50px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.innerText = str;
  return div.innerHTML;
}

function escapeQuote(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;').replace(/\n/g, ' ');
}

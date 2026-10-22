# Procucev AI Tech Hiring Assignment: Real-Time Multilingual Speech-to-Text Service

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![WebSocket](https://img.shields.io/badge/Real--time-WebSocket-purple.svg)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
[![Docker](https://img.shields.io/badge/Deployment-Docker%20%7C%20Render-blueviolet.svg)](https://www.docker.com/)

A production-grade, full-stack **Real-time Multilingual Speech-to-Text (STT)** web application engineered for code-switching sentences (such as mixtures of **Hindi & English / Hinglish**), featuring a relational database backend (**MySQL**), real-time bidirectional streaming (**WebSocket**), interactive waveform visualization (**Web Audio API**), **Text-to-Speech (TTS)** playback, and a modern glassmorphic dashboard with complete **CRUD operations**.

---

## 🌟 Key Highlights

1. **Multilingual Real-Time Transcription (Hinglish Support)**:
   - Transcribes natural code-switching sentences containing both Hindi and English (e.g., *"Hello team, kal meeting me hum project status update discuss karenge"*).
   - Real-time token streaming with sub-second interim word displays and confidence scoring.
   - Dual-engine architecture:
     - **Live Multilingual Streaming Engine**: Zero-latency, browser-native phonetic processing with real-time token classification.
     - **Gemini Live 3.1 / Flash Multilingual AI Engine**: Multimodal audio transcription and punctuation refinement.
     - **Whisper Multilingual Mode**: Open-source model compatibility.

2. **Relational Database Storage & Full CRUD Operations**:
   - Persists all transcriptions into a **MySQL** database with schema optimizations, word counting, duration logging, and audio playback data.
   - **Create**: Instant or manual save of transcribed text, detected language, and microphone recordings.
   - **Read**: Live search across titles and content, language filter pills (`All`, `Hinglish`, `English`, `Hindi`), and paginated history.
   - **Update**: Interactive glassmorphic modal to edit titles, transcribed text, tags, and languages.
   - **Delete**: Single deletion and bulk deletion with checkbox selection.
   - **Export**: One-click export to **CSV**, **JSON**, and **TXT** formats.

3. **Text-to-Speech (TTS) & Audio Replay**:
   - Built-in speech synthesis player enables listening back to any transcription in native Hindi or English voices.
   - Replay original recorded voice snippets recorded during the session.

4. **Interactive Glassmorphic Visualizer**:
   - Real-time pulsing frequency waveform rendered on HTML5 canvas via Web Audio API `AnalyserNode`.
   - Live timer, word counter, and bilingual keyword badges (separating detected Hindi vs English tokens).
   - Real-time analytics cards: Total Transcriptions, Words Transcribed, Hinglish Ratio, and Speaking Time.

---

## 🏗️ Architecture & Flow

```
[ Microphone Input ]
         │
         ▼
[ Web Audio API Analyser ] ──> Real-time Waveform Canvas
         │
         ├───> [ Web Speech Multilingual Engine (hi-IN / en-IN) ]
         │                   │ (Interim & Final Tokens)
         │                   ▼
         └───> [ WebSocket Stream: /ws/live-transcribe ]
                             │
                             ▼
                 [ Spring Boot 3.4.3 Backend ]
                 ├── Linguistic Analysis (Hindi/English/Hinglish)
                 ├── Gemini Live 3.1 AI API Integration
                 └── Hibernate / JPA Entity Layer
                             │
                             ▼
                   [ MySQL 8.0 Database ]
                   (Table: transcriptions)
```

---

## 🗄️ Database Schema

```sql
CREATE TABLE transcriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    english_translation LONGTEXT,
    detected_language VARCHAR(100) DEFAULT 'Hinglish (Hindi + English)',
    confidence DOUBLE DEFAULT 0.95,
    duration_seconds DOUBLE DEFAULT 0.0,
    word_count INT DEFAULT 0,
    audio_data LONGTEXT,
    tags VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);
```

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java 17, 21, or 25** (`java -version`)
- **MySQL 8.0** (or Docker)
- Git

---

### Option 1: Run with Local MySQL (Recommended)

1. **Clone the repository**:
   ```bash
   git clone <YOUR_GITHUB_REPO_URL>
   cd <REPO_FOLDER>
   ```

2. **Configure Database Credentials**:
   Edit `src/main/resources/application.properties` or provide environment variables:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/procucev_transcription?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```

3. **Run the Application**:
   - **Windows**:
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS**:
     ```bash
     ./mvnw spring-boot:run
     ```

4. Open your browser and navigate to:
   ```
   http://localhost:8080/
   ```

---

### Option 2: 1-Command Run with Docker Compose

Run both the Spring Boot service and a isolated MySQL 8.0 container simultaneously:

```bash
docker compose up --build
```
The application will wait for MySQL to complete health checks, automatically create the database tables, and serve the application at `http://localhost:8080/`.

---

### Option 3: Zero-Config Standalone Run (H2 In-Memory Fallback)

If MySQL is temporarily unavailable, you can launch the app instantly using the built-in standalone profile:
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```
Or execute the pre-packaged JAR directly:
```powershell
java -jar target/transcription-service-1.0.0.jar --spring.profiles.active=h2
```

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/transcriptions` | Paginated transcriptions (with `search` & `language` filters) |
| `GET` | `/api/transcriptions/all` | List all stored transcriptions |
| `GET` | `/api/transcriptions/{id}` | Retrieve single transcription details |
| `POST` | `/api/transcriptions` | Save a new transcription to MySQL |
| `PUT` | `/api/transcriptions/{id}` | Update transcription content, title, or tags |
| `DELETE` | `/api/transcriptions/{id}` | Delete a transcription by ID |
| `POST` | `/api/transcriptions/bulk-delete` | Batch delete multiple transcriptions |
| `GET` | `/api/transcriptions/stats` | Retrieve aggregate metrics (Total Words, Hinglish %, Speaking Time) |
| `GET` | `/api/transcriptions/export?format=csv` | Export all records as CSV |
| `GET` | `/api/transcriptions/export?format=json` | Export all records as JSON |
| `GET` | `/api/transcriptions/export?format=txt` | Export all records as plain text |
| `POST` | `/api/ai/detect-language` | Linguistic analysis detecting Hindi vs English vs Hinglish |
| `POST` | `/api/ai/transcribe-audio` | Cloud transcription via Gemini Live API |
| `POST` | `/api/ai/refine` | AI punctuation and formatting refinement |

### WebSocket Real-time Endpoint
- `ws://localhost:8080/ws/live-transcribe`
  - Send: `{"action": "STREAM_INTERIM", "text": "...", "duration": 4.5}`
  - Receive: Real-time token breakdown, confidence score, and language classification.

---

## ☁️ Cloud Deployment (Render / Railway)

This repository includes a production multi-stage `Dockerfile` and `render.yaml` configuration.

### Deploying to Render:
1. Push your repository to GitHub.
2. Sign in to [Render](https://render.com/).
3. Click **New +** -> **Blueprint**.
4. Connect this GitHub repository. Render will automatically detect `render.yaml`, provision a managed MySQL instance, and deploy the Spring Boot container.

---

## 🛠️ Tech Stack Breakdown

- **Backend**: Spring Boot 3.4.3, Spring Data JPA, Spring WebSocket, Tomcat
- **Database**: MySQL 8.0 (with connection pooling via HikariCP)
- **Frontend**: HTML5, Vanilla JavaScript (ES6+), Vanilla CSS3 (Glassmorphism design system)
- **Audio APIs**: Web Speech API (`SpeechRecognition`), Web Audio API (`AudioContext`, `AnalyserNode`), MediaStream Recording API
- **AI / Multilingual Engines**: Google Gemini Live / Flash API, Custom Linguistic Code-Switching Tokenizer
- **DevOps**: Docker, Docker Compose, Maven Wrapper

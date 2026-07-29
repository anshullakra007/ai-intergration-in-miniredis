# ⚡ MiniRedis - Custom In-Memory Key-Value Store

![Java](https://img.shields.io/badge/Java-21-orange)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![Concurrency](https://img.shields.io/badge/Feature-Multi_Threaded-purple)
![Live](https://img.shields.io/badge/Status-Live_Deployed-success)

MiniRedis is a lightweight, multi-threaded key-value store built from scratch using **Raw Java Sockets** (No frameworks). It mimics the core functionality of Redis, handling concurrent client connections via a custom TCP protocol.

🚀 **Live Server Address:** `miniredis.onrender.com` (Requires TCP Client)

---

## 🚀 Tech Stack
* **Language:** Java 21 (Core)
* **Networking:** `java.net.ServerSocket`, `java.net.Socket` (TCP)
* **Concurrency:** `ExecutorService` (Cached Thread Pool)
* **Storage:** `ConcurrentHashMap` (Thread-safe memory)
* **Deployment:** Docker + Render Cloud

---

## 🛠️ Architecture
1.  **TCP Listener:** The server listens on a dynamic port (configured via `ENV` or defaults to `6379`).
2.  **Thread Pool:** Uses a `CachedThreadPool` to spawn a worker thread for every new client connection, ensuring non-blocking I/O.
3.  **Data Store:** A global `ConcurrentHashMap` provides thread-safe `GET` and `SET` operations.
4.  **Protocol:** Custom text-based protocol:
    * `SET key value` → Stores data.
    * `GET key` → Retrieves data.

---

## 📸 Usage

### 1. Run Locally (Java)
```bash
javac Main.java
java Main
# Server starts on port 6379


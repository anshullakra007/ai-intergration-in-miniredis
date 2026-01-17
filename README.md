# ⚡ MiniRedis - Custom In-Memory Key-Value Store

MiniRedis is a lightweight, multi-threaded key-value store built from scratch using **Raw Java Sockets** (No frameworks). It mimics the core functionality of Redis, handling concurrent client connections via a custom TCP protocol.

## 🚀 Tech Stack
* **Language:** Java (Core)
* **Networking:** `java.net.ServerSocket`, `java.net.Socket`
* **Concurrency:** `ExecutorService` (Cached Thread Pool)
* **Storage:** `ConcurrentHashMap` (Thread-safe memory)

## 🛠️ Architecture
* **TCP Server:** Listens on Port `6379`.
* **Protocol:** Text-based protocol (similar to RESP).
  * `SET key value` → Stores data.
  * `GET key` → Retrieves data.
* **Thread-Per-Client:** Uses a thread pool to handle multiple clients simultaneously without blocking.

## 📸 Usage
1. **Start the Server:**
   ```bash
   javac Main.java && java Main
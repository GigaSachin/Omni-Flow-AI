# ⚡ OmniFlow AI — On-Device Contextual Coprocessor
> **Zero-Latency, 100% Offline AI Coprocessor built for iQOO & Qualcomm Snapdragon NPU**

[![iQOO Hackathon 2026](https://img.shields.io/badge/iQOO_Hackathon-Productivity_Track-FF6B00?style=for-the-badge)](https://iqoo.reskilll.com)
[![Offline AI](https://img.shields.io/badge/Inference-100%25_On--Device-00F0FF?style=for-the-badge)](#)
[![Snapdragon NPU](https://img.shields.io/badge/Hardware-Snapdragon_NPU_Accelerated-purple?style=for-the-badge)](#)

---

## 📱 Project Overview
**OmniFlow** is a phone-first on-device AI coprocessor designed to eliminate cloud dependency, network latency, and corporate privacy risks. Built specifically for the **iQOO Hackathon 2026 (Productivity Track)**, OmniFlow runs quantized local SLMs directly on the smartphone's **Snapdragon NPU/GPU**, enabling instantaneous context parsing, meeting minutes extraction, and offline code debugging.

---

## 🎯 Key Hackathon Dimensions & Alignment
- **End Product Quality (30%):** Native Android app built using Kotlin, Jetpack Compose, and Material 3 with a Cyber-HUD telemetry dashboard.
- **Novelty & Impact (20%):** Air-gapped, zero-trust local inference—sensitive enterprise context never leaves the device.
- **Creative Phone Use (15%):** Hardware-driven triage leveraging Snapdragon NPU acceleration, on-device Speech-to-Task, and screen crash OCR.
- **Technical Depth (15%):** Quantized 4-bit (INT4) SLM (Meta Llama-3.2-1B-Instruct) runtime via MediaPipe GenAI / ExecuTorch delegates.
- **Office Kit Usage (10%):** Sidecar Mode seamlessly bridges desktop clipboards and error logs to the phone via Vivo Office Kit.
- **Red Light Phase Ready:** Fully autonomous; runs independently when laptops are powered down.

---

## 🛠 Tech Stack
- **Framework:** Android SDK 34, Kotlin, Jetpack Compose
- **On-Device Runtime:** Google MediaPipe GenAI Tasks API (`tasks-genai:0.10.14`)
- **Model:** Meta Llama-3.2-1B-Instruct (4-bit Quantized INT4) / Gemma-2B
- **Hardware Acceleration:** Qualcomm Snapdragon NPU & Adreno GPU
- **Sidecar Connectivity:** Vivo Office Kit P2P Bridge

---

## 📄 Documentation & Links
- **Product Requirement Document (PRD):** [View Google Docs PRD](https://docs.google.com/document/d/10zammjYY_e-rWbuMsHakpYuY3Hn8SR_NVAP8Jh8jGa8/edit)
- **Team:** **Error Ninjas** (Sachin Singh, Piyush Mehta, Navin Kumar Sah)

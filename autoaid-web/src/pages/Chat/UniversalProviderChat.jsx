// UNIVERSAL PROVIDER CHAT — FINAL FIXED VERSION
import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import socket from "../../lib/chatSocket";
import { useAuth } from "../../context/AuthContext";
import { useParams } from "react-router-dom";
import "../../styles/UniversalChat.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function UniversalProviderChat() {
  const { id: requestId } = useParams();
  const { user } = useAuth();

  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [uploading, setUploading] = useState(false);
  const [typingUsers, setTypingUsers] = useState({});
  const boxRef = useRef(null);
  const fileRef = useRef(null);

  const providerId = user?._id || user?.id;

  const scrollToBottom = () => {
    setTimeout(() => {
      if (boxRef.current) {
        boxRef.current.scrollTop = boxRef.current.scrollHeight;
      }
    }, 60);
  };

  // ---------------------------------------------------
  // LOAD CHAT HISTORY
  // ---------------------------------------------------
  const loadMessages = async () => {
    try {
      const res = await axios.get(`${BASE}/api/chat/${requestId}`);
      setMessages(res.data || []);
      scrollToBottom();
    } catch (err) {
      console.error("LOAD CHAT ERROR:", err);
    }
  };

  // ---------------------------------------------------
  // SOCKET INIT
  // ---------------------------------------------------
  useEffect(() => {
    if (!requestId) return;

    if (!socket.connected) socket.connect();
    socket.emit("joinChat", { requestId });

    socket.on("chat:message", (msg) => {
      if (msg.requestId !== requestId) return;
      setMessages((prev) => [...prev, msg]);
      scrollToBottom();
    });

    socket.on("chat:typing", ({ senderId, typing, requestId: rid }) => {
      if (rid !== requestId) return;
      setTypingUsers((prev) => ({ ...prev, [senderId]: typing }));
    });

    // mark all messages as seen by provider
    socket.emit("chat:seen", { requestId, userId: providerId });

    loadMessages();

    return () => {
      socket.emit("leaveChat", { requestId });
      socket.off("chat:message");
      socket.off("chat:typing");
    };
  }, [requestId]);

  // ---------------------------------------------------
  // HANDLE TYPING
  // ---------------------------------------------------
  const handleTyping = (value) => {
    setText(value);

    socket.emit("chat:typing", {
      requestId,
      senderId: providerId,
      typing: value.length > 0,
    });
  };

  // ---------------------------------------------------
  // SEND TEXT MESSAGE
  // ---------------------------------------------------
  const sendMessage = async () => {
    if (!text.trim()) return;

    const msgPayload = {
      sender: "provider",
      text,
      senderId: providerId,
      requestId,
    };

    // optimistic UI
    setMessages((prev) => [...prev, { ...msgPayload, time: new Date() }]);
    setText("");
    scrollToBottom();

    try {
      const res = await axios.post(`${BASE}/api/chat/${requestId}`, msgPayload);
      socket.emit("chat:message", { ...res.data, requestId });
    } catch (err) {
      console.error("SEND MESSAGE ERROR:", err);
    }
  };

  // ---------------------------------------------------
  // FILE UPLOAD
  // ---------------------------------------------------
  const handleFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);

    try {
      const form = new FormData();
      form.append("file", file);
      form.append("requestId", requestId);

      const uploadRes = await axios.post(`${BASE}/api/uploads`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      const attachment = { attachments: [uploadRes.data] };

      const msgPayload = {
        sender: "provider",
        text: "",
        senderId: providerId,
        meta: attachment,
        requestId,
      };

      const res = await axios.post(`${BASE}/api/chat/${requestId}`, msgPayload);
      socket.emit("chat:message", { ...res.data, requestId });
    } catch (err) {
      console.error("FILE UPLOAD ERROR:", err);
    } finally {
      setUploading(false);
    }
  };

  // ---------------------------------------------------
  // UI
  // ---------------------------------------------------
  return (
    <div className="universal-chat-root">

      <div className="chat-header">
        <button className="back-btn" onClick={() => window.history.back()}>
          ← Back
        </button>

        <h3>Provider Chat</h3>

        {Object.values(typingUsers).some((v) => v) && (
          <div className="typing">Typing…</div>
        )}
      </div>

      <div className="chat-box" ref={boxRef}>
        {messages.length === 0 ? (
          <p className="empty">No messages yet</p>
        ) : (
          messages.map((m, i) => {
            const mine = m.senderId === providerId;
            return (
              <div
                key={m._id || i}
                className={`msg ${mine ? "me" : "them"}`}
              >
                {/* Attachments */}
                {m.meta?.attachments?.map((a, idx) => (
                  <a
                    key={idx}
                    href={a.url}
                    target="_blank"
                    rel="noreferrer"
                    className="attachment"
                  >
                    📎 {a.filename || a.originalname}
                  </a>
                ))}

                {/* Text */}
                {m.text && <div className="text">{m.text}</div>}

                {/* Time + ticks */}
                <div className="meta-row">
                  <div className="time">
                    {new Date(m.time || Date.now()).toLocaleTimeString()}
                  </div>

                  {mine && (
                    <div className="seen">
                      {m.seenBy?.length > 0 ? "✓✓" : "✓"}
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="chat-input">
        <input
          value={text}
          onChange={(e) => handleTyping(e.target.value)}
          placeholder="Type a message…"
          onKeyDown={(e) => e.key === "Enter" && sendMessage()}
        />

        <input
          type="file"
          ref={fileRef}
          style={{ display: "none" }}
          onChange={handleFile}
        />

        <button onClick={() => fileRef.current.click()} disabled={uploading}>
          📎
        </button>

        <button onClick={sendMessage} disabled={!text.trim() || uploading}>
          ➤
        </button>
      </div>
    </div>
  );
}

// UniversalUserChat.jsx
import React, { useEffect, useRef, useState } from "react";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useParams } from "react-router-dom";
import socket from "../../lib/chatSocket"; // your singleton socket (see notes if you don't have one)
import "../../styles/UniversalChat.css";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:5001";

export default function UniversalUserChat() {
  const { requestId } = useParams();
  const { user } = useAuth();

  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");
  const [typingUsers, setTypingUsers] = useState({});
  const [uploading, setUploading] = useState(false);
  const boxRef = useRef(null);
  const fileRef = useRef(null);
  const localUserId = user?._id || user?.id;

  const scrollToBottom = () => {
    if (boxRef.current) {
      boxRef.current.scrollTop = boxRef.current.scrollHeight;
    }
  };

  // load chat history
  const loadMessages = async () => {
    try {
      const res = await axios.get(`${BASE}/api/chat/${requestId}`);
      setMessages(res.data || []);
      setTimeout(scrollToBottom, 80);
    } catch (err) {
      console.error("LOAD CHAT ERROR:", err);
    }
  };

  // socket init & listeners
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
      // clear typing state after 5s (safety)
      if (typing) {
        setTimeout(() => {
          setTypingUsers((prev) => ({ ...prev, [senderId]: false }));
        }, 5000);
      }
    });

    // Optionally handle "seen" or "delivered" events
    socket.on("chat:seen", ({ requestId: rid, messageId, userId }) => {
      if (rid !== requestId) return;
      setMessages((prev) =>
        prev.map((m) =>
          (m._id === messageId || m.id === messageId) ? { ...m, seenBy: [...(m.seenBy || []), userId] } : m
        )
      );
    });

    loadMessages();

    // mark messages as seen when opening
    socket.emit("chat:seen", { requestId, userId: localUserId });

    return () => {
      socket.emit("leaveChat", { requestId });
      socket.off("chat:message");
      socket.off("chat:typing");
      socket.off("chat:seen");
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestId]);

  // typing emitter
  const handleTyping = (value) => {
    setText(value);
    socket.emit("chat:typing", {
      requestId,
      senderId: localUserId,
      typing: value.length > 0,
    });
  };

  // send text message
  const sendMessage = async () => {
    if (!text.trim()) return;

    const msgPayload = {
      sender: "user",
      text,
      senderId: localUserId,
      requestId,
    };

    // optimistic UI
    setMessages((prev) => [...prev, { ...msgPayload, time: new Date() }]);
    setText("");
    scrollToBottom();

    try {
      const res = await axios.post(`${BASE}/api/chat/${requestId}`, msgPayload);
      // server returns saved message; broadcast via socket
      socket.emit("chat:message", { ...res.data, requestId });
    } catch (err) {
      console.error("SEND ERROR:", err);
    }
  };

  // file upload
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

      const attachment = {
        attachments: [uploadRes.data],
      };

      const msgPayload = {
        sender: "user",
        text: "",
        senderId: localUserId,
        meta: attachment,
        requestId,
      };

      const res = await axios.post(`${BASE}/api/chat/${requestId}`, msgPayload);
      socket.emit("chat:message", { ...res.data, requestId });
    } catch (err) {
      console.error("UPLOAD ERROR:", err);
    } finally {
      setUploading(false);
    }
  };

  // keyboard enter
  const onKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className="universal-chat-root">
      <div className="chat-header">
        <button className="back-btn" onClick={() => window.history.back()}>
          ← Back
        </button>
        <h3>Support Chat</h3>
        <div className="header-right">
          {Object.keys(typingUsers).some((k) => typingUsers[k]) && (
            <div className="typing">Typing…</div>
          )}
        </div>
      </div>

      <div className="chat-box" ref={boxRef}>
        {messages.length === 0 ? (
          <p className="empty">No messages yet</p>
        ) : (
          messages.map((m, i) => {
            const mine = (m.senderId === localUserId);
            return (
              <div key={m._id || m.id || i} className={`msg ${mine ? "me" : "them"}`}>
                {m.meta?.attachments?.map((a, idx) => (
                  <a key={idx} href={a.url} target="_blank" rel="noreferrer" className="attachment">
                    📎 {a.filename || a.originalname || a.name}
                  </a>
                ))}

                {m.text && <div className="text">{m.text}</div>}

                <div className="meta-row">
                  <div className="time">{new Date(m.time || Date.now()).toLocaleTimeString()}</div>
                  {mine && (
                    <div className="seen">
                      {m.seenBy && Array.isArray(m.seenBy) && m.seenBy.length > 0 ? "✓✓" : "✓"}
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="chat-input">
        <textarea
          value={text}
          onChange={(e) => handleTyping(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="Type a message..."
        />

        <input
          type="file"
          ref={fileRef}
          style={{ display: "none" }}
          onChange={handleFile}
        />

        <div className="input-actions">
          <button onClick={() => fileRef.current && fileRef.current.click()} disabled={uploading}>📎</button>
          <button onClick={sendMessage} disabled={!text.trim() && !uploading}>➤</button>
        </div>
      </div>
    </div>
  );
}

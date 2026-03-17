import React, { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import axios from "axios";
import "./AdminChat.css"; // optional styling

export default function AdminChat() {
  const { requestId } = useParams();
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState("");
  const boxRef = useRef();

  // Auto scroll to bottom
  const scroll = () => {
    if (boxRef.current) {
      boxRef.current.scrollTop = boxRef.current.scrollHeight;
    }
  };

  // Load chat messages
  const loadChat = async () => {
    try {
      const res = await axios.get(`/api/chat/${requestId}`);
      setMessages(res.data.chat || []);
      setTimeout(scroll, 80);
    } catch (err) {
      console.error("admin load chat", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadChat();
    const iv = setInterval(loadChat, 4000); // polling OR replace with socket.io
    return () => clearInterval(iv);
  }, [requestId]);

  // Send message as ADMIN
  const send = async () => {
    if (!text.trim()) return;

    const payload = {
      sender: "admin",
      text: text.trim(),
      meta: { note: "admin reply" },
    };

    try {
      await axios.post(`/api/chat/${requestId}`, payload);

      // Add message locally instantly
      setMessages((m) => [
        ...m,
        { ...payload, time: new Date(), requestId },
      ]);

      setText("");
      setTimeout(scroll, 50);
    } catch (err) {
      console.error("admin send", err);
    }
  };

  if (loading) return <p style={{ textAlign: "center" }}>Loading chat…</p>;

  return (
    <div className="universal-chat-root autoaid-style">
      <div className="chat-header">Admin Conversation</div>

      <div className="chat-box" ref={boxRef}>
        {messages.length === 0 ? (
          <p className="muted">No messages yet</p>
        ) : (
          messages.map((m, i) => (
            <div
              key={i}
              className={`msg ${
                m.sender === "user"
                  ? "user"
                  : m.sender === "provider"
                  ? "provider"
                  : "admin"
              }`}
            >
              <div className="msg-text">{m.text}</div>
              <div className="msg-time">
                {new Date(m.time || m.createdAt || Date.now()).toLocaleTimeString()}
              </div>
            </div>
          ))
        )}
      </div>

      <div className="chat-input">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") send();
          }}
          placeholder="Reply as admin..."
        />
        <button onClick={send}>Send</button>
      </div>
    </div>
  );
}

import React, { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import {
  FiArrowLeft,
  FiMessageSquare,
  FiRefreshCw,
  FiSend,
} from "react-icons/fi";
import "./AdminChat.css";

export default function AdminChat() {
  const { requestId } = useParams();
  const navigate = useNavigate();

  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);

  const boxRef = useRef(null);

  const scroll = () => {
    if (boxRef.current) {
      boxRef.current.scrollTop = boxRef.current.scrollHeight;
    }
  };

  const loadChat = async () => {
    try {
      const res = await axios.get(`/api/chat/${requestId}`);
      setMessages(Array.isArray(res.data?.chat) ? res.data.chat : []);
      setTimeout(scroll, 80);
    } catch (err) {
      console.error("admin load chat", err);
      setMessages([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadChat();
    const iv = setInterval(loadChat, 4000);
    return () => clearInterval(iv);
  }, [requestId]);

  const send = async () => {
    if (!text.trim() || sending) return;

    const payload = {
      sender: "admin",
      text: text.trim(),
      meta: { note: "admin reply" },
    };

    try {
      setSending(true);

      await axios.post(`/api/chat/${requestId}`, payload);

      setMessages((m) => [
        ...m,
        { ...payload, time: new Date(), requestId },
      ]);

      setText("");
      setTimeout(scroll, 50);
    } catch (err) {
      console.error("admin send", err);
    } finally {
      setSending(false);
    }
  };

  const getSenderLabel = (sender) => {
    if (sender === "user") return "User";
    if (sender === "provider") return "Provider";
    return "Admin";
  };

  const getSenderClass = (sender) => {
    if (sender === "user") return "user";
    if (sender === "provider") return "provider";
    return "admin";
  };

  if (loading) {
    return (
      <div className="ac-page">
        <main className="ac-container">
          <div className="ac-loading-box">Loading chat...</div>
        </main>
      </div>
    );
  }

  return (
    <div className="ac-page">
      <main className="ac-container">
        <section className="ac-hero">
          <div className="ac-hero-left">
            <span className="ac-kicker">Admin / Conversation Room</span>
            <h1>Admin Conversation</h1>
            <p>
              View the full conversation thread, monitor user and provider
              communication, and reply directly as admin.
            </p>

            <div className="ac-hero-mini">
              <div className="ac-mini-box">
                <span>Request ID</span>
                <strong>{requestId}</strong>
              </div>
              <div className="ac-mini-box">
                <span>Messages</span>
                <strong>{messages.length}</strong>
              </div>
              <div className="ac-mini-box">
                <span>Sync</span>
                <strong>Every 4s</strong>
              </div>
            </div>
          </div>

          <div className="ac-hero-right">
            <button
              className="ac-btn ac-btn-light"
              onClick={() => navigate("/admin/chats")}
              type="button"
            >
              <FiArrowLeft />
              <span>Back to Conversations</span>
            </button>

            <button className="ac-btn ac-btn-primary" onClick={loadChat} type="button">
              <FiRefreshCw />
              <span>Refresh Chat</span>
            </button>
          </div>
        </section>

        <section className="ac-chat-card">
          <div className="ac-chat-head">
            <div className="ac-chat-title">
              <FiMessageSquare />
              <span>Conversation Thread</span>
            </div>
          </div>

          <div className="ac-chat-box" ref={boxRef}>
            {messages.length === 0 ? (
              <div className="ac-empty-box">No messages yet</div>
            ) : (
              messages.map((m, i) => (
                <div
                  key={i}
                  className={`ac-message-row ${getSenderClass(m.sender)}`}
                >
                  <div className="ac-message-meta">
                    <span className={`ac-sender-badge ${getSenderClass(m.sender)}`}>
                      {getSenderLabel(m.sender)}
                    </span>
                    <span className="ac-message-time">
                      {new Date(
                        m.time || m.createdAt || Date.now()
                      ).toLocaleTimeString()}
                    </span>
                  </div>

                  <div className={`ac-message-bubble ${getSenderClass(m.sender)}`}>
                    <div className="ac-message-text">{m.text}</div>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="ac-chat-input-wrap">
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") send();
              }}
              placeholder="Reply as admin..."
            />
            <button onClick={send} disabled={sending || !text.trim()} type="button">
              <FiSend />
              <span>{sending ? "Sending..." : "Send"}</span>
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
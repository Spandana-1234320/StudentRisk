import { useState, useRef, useEffect } from 'react';
import API from '../api';
import { useAuth } from '../context/AuthContext';

const INITIAL_MESSAGES = [
  { role: 'SYSTEM', text: 'Hi! I am AcademiQ Assistant. How can I help you today?' }
];

export default function Chatbot() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState(INITIAL_MESSAGES);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isOpen]);

  useEffect(() => {
    if (isOpen) {
      setMessages(INITIAL_MESSAGES);
      setInput('');
      setLoading(false);
    }
  }, [isOpen]);

  if (!user) return null; // Only show if logged in

  const handleSend = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;

    const userMessage = input;
    const requestMessages = [...messages, { role: 'USER', text: userMessage }];
    setInput('');
    setMessages(prev => [...prev, { role: 'USER', text: userMessage }]);
    setLoading(true);

    try {
      const res = await API.post('/chat/send', { message: userMessage, messages: requestMessages });
      // Remove any markdown asterisks/formatting from raw text for simple UI display
      let aiResponse = res.data.response.replace(/\*\*/g, ''); 
      setMessages(prev => [...prev, { role: 'SYSTEM', text: aiResponse }]);
    } catch (err) {
      const status = err.response?.status;
      const errorMessage =
        status === 401 || status === 403
          ? 'Your session expired or access was denied. Please log in again.'
          : err.response?.data?.response ||
            err.response?.data?.message ||
            'I could not connect right now. Please check the backend server and AI configuration.';

      setMessages(prev => [...prev, { role: 'SYSTEM', text: errorMessage }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="chatbot-wrapper">
      {isOpen ? (
        <div className="chatbot-window slide-up">
          <div className="chatbot-header">
            <div>
              <div style={{ fontWeight: 600, fontSize: '1rem' }}>🤖 AcademiQ AI</div>
              <div style={{ fontSize: '0.75rem', opacity: 0.8 }}>Project-aware AI assistant</div>
            </div>
            <button className="btn btn-ghost" onClick={() => setIsOpen(false)} style={{ padding: '0.2rem' }}>
              ✕
            </button>
          </div>
          
          <div className="chatbot-messages">
            {messages.map((m, idx) => (
              <div key={idx} className={`chat-bubble-container ${m.role === 'USER' ? 'user' : 'system'}`}>
                <div className={`chat-bubble ${m.role === 'USER' ? 'user-bubble' : 'system-bubble'}`}>
                  {m.text}
                </div>
              </div>
            ))}
            {loading && (
              <div className="chat-bubble-container system">
                <div className="chat-bubble system-bubble pulse">Thinking...</div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <form onSubmit={handleSend} className="chatbot-input-area">
            <input 
              type="text" 
              placeholder="Ask about risks, scores, advice..." 
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={loading}
              className="chat-input"
            />
            <button type="submit" disabled={!input.trim() || loading} className="btn btn-primary" style={{ padding: '0.5rem 1rem' }}>
              Send
            </button>
          </form>
        </div>
      ) : (
        <button className="chatbot-fab pulse" onClick={() => setIsOpen(true)}>
          💬 Ask AI
        </button>
      )}
    </div>
  );
}

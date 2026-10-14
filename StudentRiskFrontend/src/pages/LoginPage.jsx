import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import API from '../api';

export default function LoginPage() {
  const [form, setForm] = useState({ username: '', password: '', role: 'STUDENT' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await API.post('/auth/login', form);
      login(res.data);
      const role = res.data.role;
      if (role === 'ADMIN') navigate('/admin');
      else if (role === 'FACULTY') navigate('/faculty');
      else navigate('/student');
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        (typeof err.response?.data === 'string' ? err.response.data : null) ||
        err.message ||
        'Login failed.';
      setError(typeof msg === 'string' ? msg : 'Login failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card fade-in">
        <div className="login-header">
          <div className="login-icon">✦</div>
          <h1 className="login-title">CampusHub</h1>
          <p className="login-subtitle">Your Campus. Your Knowledge. One Intelligent Space.</p>
        </div>

        {error && (
          <div className="alert-banner error">⚠️ {error}</div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Role Selection */}
          <div className="form-group" style={{ marginBottom: '1.2rem' }}>
            <label className="form-label">Sign in as:</label>
            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.25rem' }}>
              {['STUDENT', 'FACULTY', 'ADMIN'].map((r) => (
                <button
                  type="button"
                  key={r}
                  onClick={() => setForm({ ...form, role: r })}
                  style={{
                    flex: 1,
                    padding: '0.6rem 0.5rem',
                    borderRadius: 'var(--radius-md)',
                    border: form.role === r
                      ? '2px solid var(--primary)'
                      : '2px solid var(--border)',
                    background: form.role === r ? 'var(--primary)' : 'var(--bg-surface)',
                    color: form.role === r ? '#fff' : 'var(--text-secondary)',
                    fontWeight: 600,
                    fontSize: '0.8rem',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                  }}
                >
                  {r === 'STUDENT' ? '🎓' : r === 'FACULTY' ? '👨‍🏫' : '🛡️'} {r}
                </button>
              ))}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Username</label>
            <input
              className="form-input"
              placeholder="Enter your username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading}
            style={{ width: '100%', justifyContent: 'center', marginTop: '0.5rem', padding: '0.8rem' }}>
            {loading ? '⏳ Signing in...' : '🔐 Sign In'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Don&apos;t have an account?{' '}
          <Link to="/register" style={{ color: 'var(--primary-light)', fontWeight: 600, textDecoration: 'none' }}>
            Create Account
          </Link>
        </div>
      </div>
    </div>
  );
}

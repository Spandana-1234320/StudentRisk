import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import API from '../api';

export default function RegisterPage() {
  const [form, setForm] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    role: 'STUDENT',
    rollNumber: '',
    department: '',
    currentSemester: 1,
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        username: form.username,
        password: form.password,
        role: form.role,
        ...(form.role === 'STUDENT' && {
          rollNumber: form.rollNumber,
          department: form.department,
          currentSemester: Number(form.currentSemester),
        }),
        ...(form.role === 'FACULTY' && {
          designation: form.designation || 'Professor',
        }),
      };
      const res = await API.post('/auth/register', payload);
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
        'Registration failed.';
      setError(typeof msg === 'string' ? msg : 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card fade-in" style={{ maxWidth: '460px' }}>
        <div className="login-header">
          <div className="login-icon">✦</div>
          <h1 className="login-title">CampusHub</h1>
          <p className="login-subtitle">Create your student intelligence profile</p>
        </div>

        {error && <div className="alert-banner error">⚠️ {error}</div>}

        <form onSubmit={handleSubmit}>
          {/* Username */}
          <div className="form-group">
            <label className="form-label">Username</label>
            <input
              className="form-input"
              placeholder="Choose a username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
          </div>

          {/* Password */}
          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="Min. 6 characters"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </div>

          {/* Confirm Password */}
          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="Repeat your password"
              value={form.confirmPassword}
              onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
              required
            />
          </div>

          {/* Role Selection */}
          <div className="form-group">
            <label className="form-label">Select Role</label>
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

          {/* Student-specific fields */}
          {form.role === 'STUDENT' && (
            <div style={{
              background: 'var(--bg-surface)',
              borderRadius: 'var(--radius-md)',
              padding: '1rem',
              marginBottom: '1rem',
              border: '1px solid var(--border)',
            }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '0.75rem', fontWeight: 600 }}>
                📋 Student Details
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label className="form-label">Roll Number</label>
                <input
                  className="form-input"
                  placeholder="e.g. CS2021001"
                  value={form.rollNumber}
                  onChange={(e) => setForm({ ...form, rollNumber: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label className="form-label">Department</label>
                <select
                  className="form-input"
                  value={form.department}
                  onChange={(e) => setForm({ ...form, department: e.target.value })}
                  required
                >
                  <option value="" disabled>Select Department</option>
                  <option value="Computer Science and Engineering">Computer Science and Engineering</option>
                  <option value="Information Technology">Information Technology</option>
                  <option value="Electronics and Communication">Electronics and Communication</option>
                  <option value="Electrical Engineering">Electrical Engineering</option>
                  <option value="Mechanical Engineering">Mechanical Engineering</option>
                  <option value="Civil Engineering">Civil Engineering</option>
                </select>
              </div>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Current Semester</label>
                <input
                  type="number"
                  min="1"
                  max="8"
                  className="form-input"
                  value={form.currentSemester}
                  onChange={(e) => setForm({ ...form, currentSemester: e.target.value })}
                />
              </div>
            </div>
          )}

          {/* Faculty-specific fields */}
          {form.role === 'FACULTY' && (
            <div style={{
              background: 'var(--bg-surface)',
              borderRadius: 'var(--radius-md)',
              padding: '1rem',
              marginBottom: '1rem',
              border: '1px solid var(--border)',
            }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '0.75rem', fontWeight: 600 }}>
                👨‍🏫 Faculty Details
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Designation</label>
                <select
                  className="form-input"
                  value={form.designation || 'Professor'}
                  onChange={(e) => setForm({ ...form, designation: e.target.value })}
                >
                  <option value="Professor">Professor</option>
                  <option value="Associate Professor">Associate Professor</option>
                  <option value="Assistant Professor">Assistant Professor</option>
                  <option value="Lecturer">Lecturer</option>
                  <option value="Lab Assistant">Lab Assistant</option>
                </select>
              </div>
            </div>
          )}

          <button
            className="btn btn-primary"
            type="submit"
            disabled={loading}
            style={{ width: '100%', justifyContent: 'center', marginTop: '0.5rem', padding: '0.8rem' }}
          >
            {loading ? '⏳ Creating account...' : '🚀 Create Account'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'var(--primary-light)', fontWeight: 600, textDecoration: 'none' }}>
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}

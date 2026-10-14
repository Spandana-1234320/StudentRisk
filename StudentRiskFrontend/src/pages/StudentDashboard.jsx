import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import API from '../api';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer,
  RadarChart, Radar, PolarGrid, PolarAngleAxis
} from 'recharts';

export default function StudentDashboard() {
  const { user } = useAuth();
  const location = useLocation();
  const path = location.pathname;

  let activeTab = 'overview';
  if (path.includes('/records')) activeTab = 'records';
  else if (path.includes('/alerts') || path.includes('/interventions')) activeTab = 'alerts';

  const [risk, setRisk] = useState(null);
  const [records, setRecords] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [interventions, setInterventions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      API.get(`/student/my-risk?username=${user.username}`),
      API.get(`/student/my-records?username=${user.username}`),
      API.get(`/student/my-alerts?username=${user.username}`),
      API.get(`/student/my-interventions?username=${user.username}`),
    ]).then(([riskRes, recRes, alertRes, intRes]) => {
      setRisk(riskRes.data);
      setRecords(recRes.data);
      setAlerts(alertRes.data);
      setInterventions(intRes.data);
    }).finally(() => setLoading(false));
  }, []);

  const markRead = async (alertId) => {
    await API.put(`/student/alerts/${alertId}/read`);
    setAlerts(prev => prev.map(a => a.id === alertId ? { ...a, read: true } : a));
  };

  const radarData = risk ? [
    { subject: 'Attendance', value: risk.avgAttendance },
    { subject: 'Marks', value: risk.avgInternalMarks },
    { subject: 'Assignments', value: risk.avgAssignmentCompletion },
    { subject: 'Overall Safety', value: 100 - risk.riskScore },
  ] : [];

  const lineData = records.map((r, i) => ({ name: r.subject || `Record ${i+1}`, attendance: r.attendancePercentage, marks: r.internalMarks }));

  if (loading) return <div className="spinner" />;

  const unreadCount = alerts.filter(a => !a.read).length;

  return (
    <div className="slide-up campus-shell">
      <div className="topbar">
        <div>
          <div className="eyebrow">Campus intelligence</div>
          <div className="page-title">Good morning, {user?.username || 'student'}</div>
          <div className="page-subtitle">B.Tech • CSE • Semester 6 • 3 learning streaks</div>
        </div>
        <div className="header-actions">
          {unreadCount > 0 && <div className="status-pill danger">🔔 {unreadCount}</div>}
          <div className="avatar">{user?.username?.slice(0,2).toUpperCase()}</div>
        </div>
      </div>

      <div className="campus-hero card">
        <div className="campus-hero-header">
          <div className="hero-copy">
            <div className="hero-kicker">Ask Campus AI</div>
            <h2>Your entire campus, one question away.</h2>
            <p>Discover resources, understand difficult concepts, and find your next step with AI.</p>
          </div>

          {risk && (
            <div className="risk-spotlight">
              <div className="spotlight-metric">
                <span className="metric-value" style={{ color: risk.riskCategory === 'HIGH' ? 'var(--danger)' : risk.riskCategory === 'MEDIUM' ? 'var(--warning-dark)' : 'var(--success)' }}>{risk.riskScore}</span>
                <span className={`risk-badge ${risk.riskCategory}`}>{risk.riskCategory} risk</span>
              </div>
              <div className="mini-metrics">
                <div><strong>{risk.avgAttendance}%</strong><span>Attendance</span></div>
                <div><strong>{risk.avgInternalMarks}</strong><span>Avg Marks</span></div>
                <div><strong>{risk.avgAssignmentCompletion}%</strong><span>Assignments</span></div>
              </div>
            </div>
          )}
        </div>

        <div className="command-bar">
          <span className="command-icon">✦</span>
          <input type="text" value="" placeholder="Ask Campus AI anything..." readOnly />
          <button type="button">Send</button>
        </div>

        <div className="chip-list">
          <button type="button" className="chip">Explain deadlocks in simple terms</button>
          <button type="button" className="chip">Find previous-year AIML papers</button>
          <button type="button" className="chip">Summarize my operating systems notes</button>
          <button type="button" className="chip">Build a 7-day exam prep plan</button>
        </div>
      </div>

      {/* ─── OVERVIEW TAB ─── */}
      {activeTab === 'overview' && (
        <>
          {/* Charts */}
      <div className="grid-2 mb-4">
        <div className="card chart-card">
          <div className="chart-title">Performance Radar</div>
          <div className="chart-subtitle">Your academic profile overview</div>
          <ResponsiveContainer width="100%" height={200}>
            <RadarChart data={radarData}>
              <PolarGrid stroke="var(--border-light)" />
              <PolarAngleAxis dataKey="subject" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
              <Radar dataKey="value" stroke="var(--accent)" fill="var(--accent)" fillOpacity={0.2} />
            </RadarChart>
          </ResponsiveContainer>
        </div>

        <div className="card chart-card">
          <div className="chart-title">Subject-wise Performance</div>
          <div className="chart-subtitle">Attendance & marks per subject</div>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={lineData} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
              <XAxis dataKey="name" tick={{ fill: 'var(--text-muted)', fontSize: 10 }} />
              <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 10 }} />
              <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '8px', color: 'var(--text-primary)' }} />
              <Line type="monotone" dataKey="attendance" stroke="var(--primary)" strokeWidth={2} dot={{ fill: 'var(--primary)' }} name="Attendance %" />
              <Line type="monotone" dataKey="marks" stroke="var(--accent)" strokeWidth={2} dot={{ fill: 'var(--accent)' }} name="Marks" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
      </>
      )}

      {/* ─── ALERTS & INTERVENTIONS TAB ─── */}
      {activeTab === 'alerts' && (
      <div className="grid-2">
        <div className="card">
          <div className="section-title mb-3">🔔 My Alerts</div>
          {alerts.length === 0 ? (
            <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '1.5rem', fontSize: '0.875rem' }}>🎉 No alerts – you're doing great!</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {alerts.map(a => (
                <div key={a.id} style={{
                  padding: '0.75rem 1rem',
                  background: a.read ? 'var(--bg-input)' : 'rgba(255,77,109,0.07)',
                  border: `1px solid ${a.read ? 'var(--border-light)' : 'rgba(255,77,109,0.2)'}`,
                  borderRadius: 'var(--radius-sm)',
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem'
                }}>
                  <div>
                    <div style={{ fontSize: '0.845rem', color: a.read ? 'var(--text-muted)' : 'var(--text-primary)' }}>{a.message}</div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>{new Date(a.createdAt).toLocaleString()}</div>
                  </div>
                  {!a.read && (
                    <button className="btn btn-sm btn-ghost" onClick={() => markRead(a.id)}>Mark Read</button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card">
          <div className="section-title mb-3">🤝 My Interventions</div>
          {interventions.length === 0 ? (
            <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '1.5rem', fontSize: '0.875rem' }}>No interventions assigned yet.</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {interventions.map(i => (
                <div key={i.id} style={{ padding: '0.875rem 1rem', background: 'var(--bg-surface)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-md)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
                    <span className="tag purple">{i.type?.replace('_', ' ')}</span>
                    <span className={`tag ${i.status === 'COMPLETED' ? 'teal' : ''}`}>{i.status}</span>
                  </div>
                  <div style={{ fontSize: '0.845rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>{i.description}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                    Assigned by {i.assignedBy?.username} · {new Date(i.assignedAt).toLocaleDateString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
      )}

      {/* ─── ACADEMIC RECORDS TAB ─── */}
      {activeTab === 'records' && (
      <div className="card mt-4">
        <div className="section-title mb-3">📋 My Academic Records (Marks & Attendance)</div>
        {records.length === 0 ? (
          <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '1.5rem', fontSize: '0.875rem' }}>No academic records available yet.</div>
        ) : (
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sem</th>
                  <th>Subject</th>
                  <th>Attendance</th>
                  <th>Internal Marks</th>
                  <th>Prev Sem Marks</th>
                  <th>Assignments Sub (%)</th>
                  <th>Logged Date</th>
                </tr>
              </thead>
              <tbody>
                {records.map(r => (
                  <tr key={r.id}>
                    <td><span className="tag">Sem {r.semester}</span></td>
                    <td style={{ fontWeight: 600 }}>{r.subject || 'N/A'}</td>
                    <td>
                      <div className="progress-bar-wrap mb-1" style={{ width: '80%' }}>
                        <div className="progress-bar">
                          <div className="progress-fill" style={{ width: `${r.attendancePercentage}%`, background: r.attendancePercentage < 75 ? 'var(--danger)' : 'var(--success)' }} />
                        </div>
                      </div>
                      <span style={{ fontSize: '0.75rem' }}>{r.attendancePercentage}%</span>
                    </td>
                    <td>{r.internalMarks}</td>
                    <td>{r.previousSemesterMarks}</td>
                    <td>{r.assignmentSubmissionPercentage}%</td>
                    <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{new Date(r.createdAt || Date.now()).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      )}

    </div>
  );
}

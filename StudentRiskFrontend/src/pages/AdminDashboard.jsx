import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import API from '../api';
import { useAuth } from '../context/AuthContext';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts';

const RISK_COLORS = { HIGH: '#ff4d6d', MEDIUM: '#ffd166', LOW: '#06d6a0' };

const INITIAL_DEPARTMENTS = [
  { id: 1, name: 'Computer Science and Engineering', code: 'CSE', subjects: ['Data Structures', 'Algorithms', 'Database Management', 'Operating Systems', 'Compiler Design'] },
  { id: 2, name: 'Information Technology', code: 'IT', subjects: ['Web Technologies', 'Computer Networks', 'Cloud Computing', 'Cyber Security', 'Software Engineering'] },
  { id: 3, name: 'Electronics and Communication', code: 'ECE', subjects: ['Analog Circuits', 'Digital Electronics', 'Signals & Systems', 'Microprocessors', 'VLSI Design'] },
  { id: 4, name: 'Electrical Engineering', code: 'EEE', subjects: ['Power Systems', 'Control Systems', 'Electrical Machines', 'Power Electronics', 'Circuit Theory'] },
  { id: 5, name: 'Mechanical Engineering', code: 'MECH', subjects: ['Thermodynamics', 'Fluid Mechanics', 'Machine Design', 'Heat Transfer', 'Manufacturing Tech'] },
  { id: 6, name: 'Civil Engineering', code: 'CIVIL', subjects: ['Structural Analysis', 'Geotechnical Engg', 'Transportation Engg', 'Environmental Engg', 'Surveying'] }
];

export default function AdminDashboard() {
  const { user } = useAuth();
  const location = useLocation();
  const path = location.pathname;

  let activeTab = 'overview';
  if (path.includes('/students')) activeTab = 'students';
  else if (path.includes('/faculty')) activeTab = 'faculty';
  else if (path.includes('/departments')) activeTab = 'departments';
  else if (path.includes('/reports')) activeTab = 'reports';

  const [stats, setStats] = useState(null);
  const [students, setStudents] = useState([]);
  const [allStudents, setAllStudents] = useState([]);
  const [faculty, setFaculty] = useState([]);
  const [loading, setLoading] = useState(true);

  const [departmentsList, setDepartmentsList] = useState(INITIAL_DEPARTMENTS);
  const [showDeptModal, setShowDeptModal] = useState(false);
  const [newDeptForm, setNewDeptForm] = useState({ name: '', code: '', subjects: '' });
  const [showSubjModal, setShowSubjModal] = useState(false);
  const [newSubjForm, setNewSubjForm] = useState({ deptId: '', subjectName: '' });

  const handleCreateDept = (e) => {
    e.preventDefault();
    if (!newDeptForm.name || !newDeptForm.code) return;
    const subjectsArray = newDeptForm.subjects.split(',').map(s => s.trim()).filter(Boolean);
    const newDept = {
      id: Date.now(),
      name: newDeptForm.name,
      code: newDeptForm.code,
      subjects: subjectsArray
    };
    setDepartmentsList([...departmentsList, newDept]);
    setShowDeptModal(false);
    setNewDeptForm({ name: '', code: '', subjects: '' });
  };

  const handleAddSubject = (e) => {
    e.preventDefault();
    if (!newSubjForm.deptId || !newSubjForm.subjectName) return;
    setDepartmentsList(departmentsList.map(d => {
      if (d.id === Number(newSubjForm.deptId)) {
        return { ...d, subjects: [...d.subjects, newSubjForm.subjectName] };
      }
      return d;
    }));
    setShowSubjModal(false);
    setNewSubjForm({ deptId: '', subjectName: '' });
  };

  // Fetch all required data once
  const fetchData = () => {
    setLoading(true);
    Promise.all([
      API.get('/admin/stats').catch(() => ({data: null})),
      API.get('/admin/analytics').catch(() => ({data: []})),
      API.get('/admin/students').catch(() => ({data: []})),
      API.get('/admin/faculty').catch(() => ({data: []})),
    ]).then(([statsRes, analyticsRes, stuRes, facRes]) => {
      setStats(statsRes.data);
      setStudents(analyticsRes.data);
      setAllStudents(stuRes.data);
      setFaculty(facRes.data);
    }).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleDeleteUser = async (id, name) => {
    if (window.confirm(`Are you sure you want to completely remove ${name} from the system?`)) {
      try {
        await API.delete(`/admin/users/${id}`);
        fetchData(); // reload dashboard
      } catch (err) {
        alert('Failed to delete user.');
      }
    }
  };

  const handleSeedData = async () => {
    if (window.confirm('This will randomly generate and insert 50 mock student records for system testing. Proceed?')) {
      setLoading(true);
      try {
        const res = await API.post('/admin/seed');
        alert(res.data.message || 'Seeded successfully.');
        fetchData();
      } catch (err) {
        alert('Failed to seed data.');
        setLoading(false);
      }
    }
  };

  const pieData = stats ? [
    { name: 'High Risk', value: stats.highRisk },
    { name: 'Medium Risk', value: stats.mediumRisk },
    { name: 'Low Risk', value: stats.lowRisk },
  ] : [];

  const deptData = students.reduce((acc, s) => {
    const d = s.department || 'Unknown';
    const ex = acc.find(x => x.dept === d);
    if (ex) { ex.students++; if (s.riskCategory === 'HIGH') ex.high++; }
    else acc.push({ dept: d, students: 1, high: s.riskCategory === 'HIGH' ? 1 : 0 });
    return acc;
  }, []);

  if (loading) return <div className="spinner" />;

  return (
    <div className="slide-up">
      <div className="topbar">
        <div>
          <div className="page-title">Admin Overview</div>
          <div className="page-subtitle">System-wide academic risk intelligence</div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div className="avatar">{user?.username?.slice(0,2).toUpperCase()}</div>
        </div>
      </div>

      {/* ─── OVERVIEW TAB ─── */}
      {activeTab === 'overview' && (
        <>
          {/* Stat Cards */}
          <div className="stats-grid mb-4">
            <div className="stat-card">
              <div className="stat-icon">🎓</div>
              <div className="stat-value">{stats?.totalStudents ?? 0}</div>
              <div className="stat-label">Total Students</div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">👨‍🏫</div>
              <div className="stat-value">{stats?.totalFaculty ?? 0}</div>
              <div className="stat-label">Total Faculty</div>
            </div>
            <div className="stat-card danger pulse">
              <div className="stat-icon">🚨</div>
              <div className="stat-value" style={{ color: 'var(--danger)' }}>{stats?.highRisk ?? 0}</div>
              <div className="stat-label">High Risk Students</div>
            </div>
            <div className="stat-card warning">
              <div className="stat-icon">⚠️</div>
              <div className="stat-value" style={{ color: 'var(--warning-dark)' }}>{stats?.mediumRisk ?? 0}</div>
              <div className="stat-label">Medium Risk</div>
            </div>
            <div className="stat-card success">
              <div className="stat-icon">✅</div>
              <div className="stat-value" style={{ color: 'var(--success)' }}>{stats?.lowRisk ?? 0}</div>
              <div className="stat-label">Low Risk</div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">🔔</div>
              <div className="stat-value">{stats?.unreadAlerts ?? 0}</div>
              <div className="stat-label">Unread Alerts</div>
            </div>
          </div>

          {/* Charts */}
          <div className="grid-2 mb-4">
            <div className="card chart-card">
              <div className="chart-title">Risk Distribution</div>
              <div className="chart-subtitle">Overall student risk categories</div>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={3} dataKey="value" stroke="none">
                    {pieData.map((entry) => (
                      <Cell key={entry.name} fill={RISK_COLORS[entry.name.split(' ')[0].toUpperCase()]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '8px', color: 'var(--text-primary)' }} />
                  <Legend wrapperStyle={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            <div className="card chart-card">
              <div className="chart-title">Department Analysis</div>
              <div className="chart-subtitle">Students per department vs high risk</div>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={deptData} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                  <XAxis dataKey="dept" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <Tooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '8px', color: 'var(--text-primary)' }} />
                  <Bar dataKey="students" fill="var(--primary)" radius={[4,4,0,0]} name="Total" />
                  <Bar dataKey="high" fill="var(--danger)" radius={[4,4,0,0]} name="High Risk" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}

      {/* ─── STUDENTS TAB ─── */}
      {activeTab === 'students' && (
        <>
          <div className="card mb-3" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div className="section-title">🎓 Student Records Overview</div>
              <div className="section-subtitle">Manage, add, update, or delete student profiles.</div>
            </div>
            <div style={{ display: 'flex', gap: '1rem' }}>
              <button className="btn btn-secondary" onClick={handleSeedData}>+ Seed Mock Data</button>
              <button className="btn btn-primary" onClick={() => window.location.href = '/register'}>+ Add New Student</button>
            </div>
          </div>
          {/* Students Table */}
          <div className="card mb-3">
            <div className="section-header">
              <div>
                <div className="section-title">🎓 Registered Students</div>
                <div className="section-subtitle">{allStudents.length} students in the system</div>
              </div>
            </div>
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Username</th>
                    <th>Roll Number</th>
                    <th>Department</th>
                    <th>Semester</th>
                    <th>Role</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {allStudents.map((s, i) => (
                    <tr key={s.id}>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{i + 1}</td>
                      <td style={{ fontWeight: 600 }}>👤 {s.username}</td>
                      <td><span className="tag">{s.rollNumber || '—'}</span></td>
                      <td>{s.department || '—'}</td>
                      <td>{s.currentSemester ? `Sem ${s.currentSemester}` : '—'}</td>
                      <td><span className="risk-badge LOW">STUDENT</span></td>
                      <td>
                        <button className="btn btn-sm btn-ghost" style={{ color: 'var(--danger)' }} onClick={() => handleDeleteUser(s.id, s.username)}>Delete</button>
                      </td>
                    </tr>
                  ))}
                  {allStudents.length === 0 && (
                    <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>No students registered yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ─── FACULTY TAB ─── */}
      {activeTab === 'faculty' && (
        <>
          <div className="card mb-3" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div className="section-title">👨‍🏫 Faculty Details</div>
              <div className="section-subtitle">Manage, add, or update faculty records.</div>
            </div>
          </div>
          {/* Faculty Table */}
          <div className="card mb-3">
            <div className="section-header">
              <div>
                <div className="section-title">👨‍🏫 Registered Faculty</div>
                <div className="section-subtitle">{faculty.length} faculty members in the system</div>
              </div>
            </div>
            <div className="data-table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {faculty.map((f, i) => (
                    <tr key={f.id}>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{i + 1}</td>
                      <td style={{ fontWeight: 600 }}>👨‍🏫 {f.username}</td>
                      <td><span className="risk-badge MEDIUM" style={{ color: '#ffd166', borderColor: '#ffd166' }}>FACULTY</span></td>
                      <td>
                        <button className="btn btn-sm btn-ghost" style={{ color: 'var(--danger)' }} onClick={() => handleDeleteUser(f.id, f.username)}>Delete</button>
                      </td>
                    </tr>
                  ))}
                  {faculty.length === 0 && (
                    <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>No faculty registered yet.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ─── DEPARTMENTS TAB ─── */}
      {activeTab === 'departments' && (
        <>
          <div className="card mb-3" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div className="section-title">🏢 Departments & Subjects</div>
              <div className="section-subtitle">Manage curriculum, departments, and course allocations.</div>
            </div>
            <div style={{ display: 'flex', gap: '1rem' }}>
              <button className="btn btn-secondary" onClick={() => setShowSubjModal(true)}>+ Add Subject</button>
              <button className="btn btn-primary" onClick={() => setShowDeptModal(true)}>+ Add Department</button>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
            {departmentsList.map((dept) => (
              <div key={dept.id} className="card fade-in" style={{ padding: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>{dept.name}</h3>
                  <span className="tag" style={{ background: 'var(--primary-light)', color: '#fff', fontSize: '0.7rem' }}>{dept.code}</span>
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.75rem', fontWeight: 600 }}>Core Subjects ({dept.subjects.length}):</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                  {dept.subjects.map((sub, i) => (
                    <span key={i} className="tag" style={{ display: 'inline-block' }}>{sub}</span>
                  ))}
                  {dept.subjects.length === 0 && <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No subjects added.</span>}
                </div>
              </div>
            ))}
          </div>

          {/* Add Department Modal */}
          {showDeptModal && (
            <div className="modal-overlay">
              <div className="modal-content fade-in">
                <div className="modal-header">
                  <h3 className="modal-title">Add New Department</h3>
                  <button className="modal-close" onClick={() => setShowDeptModal(false)}>×</button>
                </div>
                <div className="modal-body">
                  <form onSubmit={handleCreateDept}>
                    <div className="form-group">
                      <label className="form-label">Department Name</label>
                      <input className="form-input" placeholder="e.g. Science and Humanities" value={newDeptForm.name} onChange={e => setNewDeptForm({...newDeptForm, name: e.target.value})} required />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Department Code</label>
                      <input className="form-input" placeholder="e.g. S&H" value={newDeptForm.code} onChange={e => setNewDeptForm({...newDeptForm, code: e.target.value.toUpperCase()})} required />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Initial Subjects (Optional)</label>
                      <input className="form-input" placeholder="Comma separated, e.g. Mathematics, Physics" value={newDeptForm.subjects} onChange={e => setNewDeptForm({...newDeptForm, subjects: e.target.value})} />
                      <div className="form-hint">Separate multiple subjects with a comma.</div>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                      <button type="button" className="btn btn-secondary" onClick={() => setShowDeptModal(false)}>Cancel</button>
                      <button type="submit" className="btn btn-primary">Create Department</button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          )}

          {/* Add Subject Modal */}
          {showSubjModal && (
            <div className="modal-overlay">
              <div className="modal-content fade-in">
                <div className="modal-header">
                  <h3 className="modal-title">Add New Subject</h3>
                  <button className="modal-close" onClick={() => setShowSubjModal(false)}>×</button>
                </div>
                <div className="modal-body">
                  <form onSubmit={handleAddSubject}>
                    <div className="form-group">
                      <label className="form-label">Target Department</label>
                      <select className="form-input" value={newSubjForm.deptId} onChange={e => setNewSubjForm({...newSubjForm, deptId: e.target.value})} required>
                        <option value="" disabled>Select Department</option>
                        {departmentsList.map(d => (
                           <option key={d.id} value={d.id}>{d.name}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label className="form-label">Subject Name</label>
                      <input className="form-input" placeholder="e.g. Artificial Intelligence" value={newSubjForm.subjectName} onChange={e => setNewSubjForm({...newSubjForm, subjectName: e.target.value})} required />
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                      <button type="button" className="btn btn-secondary" onClick={() => setShowSubjModal(false)}>Cancel</button>
                      <button type="submit" className="btn btn-primary">Add Subject</button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* ─── REPORTS & ANALYTICS TAB ─── */}
      {activeTab === 'reports' && (
        <div className="card mb-3">
          <div className="section-header">
            <div>
              <div className="section-title">All Students Risk Summary</div>
              <div className="section-subtitle">{students.length} students tracked</div>
            </div>
          </div>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Roll No</th>
                  <th>Department</th>
                  <th>Sem</th>
                  <th>Attendance</th>
                  <th>Marks</th>
                  <th>Risk Score</th>
                  <th>Category</th>
                  <th>Dropout</th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => (
                  <tr key={s.studentId}>
                    <td style={{ fontWeight: 600 }}>{s.studentName}</td>
                    <td><span className="tag">{s.rollNumber}</span></td>
                    <td>{s.department}</td>
                    <td>{s.semester}</td>
                    <td>
                      <div className="progress-bar-wrap">
                        <div className="progress-bar">
                          <div className="progress-fill" style={{ width: `${s.avgAttendance}%`, background: s.avgAttendance < 75 ? 'var(--danger)' : 'var(--success)' }} />
                        </div>
                        <span className="progress-label">{s.avgAttendance}%</span>
                      </div>
                    </td>
                    <td>{s.avgInternalMarks}/100</td>
                    <td><strong style={{ color: s.riskScore >= 65 ? 'var(--danger)' : s.riskScore >= 35 ? 'var(--warning-dark)' : 'var(--success)' }}>{s.riskScore}</strong></td>
                    <td><span className={`risk-badge ${s.riskCategory}`}>{s.riskCategory === 'HIGH' ? '🔴' : s.riskCategory === 'MEDIUM' ? '🟡' : '🟢'} {s.riskCategory}</span></td>
                    <td>{s.dropoutRisk ? <span style={{ color: 'var(--danger)', fontWeight: 700 }}>⚠️ YES</span> : <span style={{ color: 'var(--success)' }}>No</span>}</td>
                  </tr>
                ))}
                {students.length === 0 && (
                  <tr><td colSpan={9} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>No student data yet. Register students and add academic records.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

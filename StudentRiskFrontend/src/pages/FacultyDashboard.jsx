import { Fragment, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import API from '../api';
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis,
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip
} from 'recharts';

const EMPTY_RECORD_FORM = {
  semester: 1,
  subject: '',
  attendancePercentage: '',
  internalMarks: '',
  previousSemesterMarks: '',
  assignmentSubmissionPercentage: '',
  lateSubmissions: 0,
  absenteePatterns: 0
};

export default function FacultyDashboard() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const path = location.pathname;

  let activeTab = 'overview';
  if (path.includes('/records')) activeTab = 'records';
  else if (path.includes('/interventions') || path.includes('/alerts')) activeTab = 'interventions';

  const [students, setStudents] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedStudentId, setExpandedStudentId] = useState(null);
  const [expandedStudent, setExpandedStudent] = useState(null);
  const [studentRecords, setStudentRecords] = useState({});
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [filter, setFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [intForm, setIntForm] = useState({ description: '', type: 'MENTORING_SESSION' });
  const [recordForm, setRecordForm] = useState(EMPTY_RECORD_FORM);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async (preserveStudentId = expandedStudentId) => {
    setLoading(true);
    try {
      const [studRes, alertRes] = await Promise.all([
        API.get('/faculty/students/risk-overview'),
        API.get('/faculty/alerts'),
      ]);

      setStudents(studRes.data);
      setAlerts(alertRes.data);

      if (preserveStudentId) {
        const updated = studRes.data.find((student) => student.studentId === preserveStudentId) || null;
        setExpandedStudent(updated);
      }
    } finally {
      setLoading(false);
    }
  };

  const filtered = students
    .filter((student) => filter === 'ALL' || student.riskCategory === filter)
    .filter((student) => {
      const query = searchTerm.trim().toLowerCase();
      if (!query) return true;
      return (
        student.studentName?.toLowerCase().includes(query) ||
        student.rollNumber?.toLowerCase().includes(query)
      );
    });

  const loadStudentRecords = async (studentId) => {
    setRecordsLoading(true);
    try {
      const response = await API.get(`/faculty/students/${studentId}/records`);
      setStudentRecords((prev) => ({ ...prev, [studentId]: response.data }));
    } finally {
      setRecordsLoading(false);
    }
  };

  const handleToggleStudent = async (student) => {
    if (expandedStudentId === student.studentId) {
      setExpandedStudentId(null);
      setExpandedStudent(null);
      setRecordForm(EMPTY_RECORD_FORM);
      return;
    }

    setExpandedStudentId(student.studentId);
    setExpandedStudent(student);
    setRecordForm(EMPTY_RECORD_FORM);

    if (!studentRecords[student.studentId]) {
      await loadStudentRecords(student.studentId);
    }
  };

  const assignIntervention = async () => {
    if (!expandedStudent) return;
    try {
      await API.post(`/faculty/students/${expandedStudent.studentId}/interventions?facultyUsername=${user.username}`, intForm);
      setShowModal(false);
      setIntForm({ description: '', type: 'MENTORING_SESSION' });
      alert('Intervention assigned successfully!');
    } catch {
      alert('Failed to assign intervention.');
    }
  };

  const handleAddRecord = async () => {
    if (!expandedStudent) return;
    try {
      await API.post(`/faculty/students/${expandedStudent.studentId}/records`, {
        ...recordForm,
        attendancePercentage: Number(recordForm.attendancePercentage),
        internalMarks: Number(recordForm.internalMarks),
        previousSemesterMarks: Number(recordForm.previousSemesterMarks || 0),
        assignmentSubmissionPercentage: Number(recordForm.assignmentSubmissionPercentage),
        lateSubmissions: Number(recordForm.lateSubmissions),
        absenteePatterns: Number(recordForm.absenteePatterns),
      });

      setRecordForm(EMPTY_RECORD_FORM);
      await Promise.all([
        loadDashboardData(expandedStudent.studentId),
        loadStudentRecords(expandedStudent.studentId),
      ]);
      alert('Record added successfully! Attendance, marks, and risk were updated.');
    } catch {
      alert('Failed to add record. Please check the values and try again.');
    }
  };

  const radarData = expandedStudent ? [
    { subject: 'Attendance', value: expandedStudent.avgAttendance },
    { subject: 'Marks', value: expandedStudent.avgInternalMarks },
    { subject: 'Assignments', value: expandedStudent.avgAssignmentCompletion },
    { subject: 'Safety', value: 100 - expandedStudent.riskScore },
  ] : [];

  if (loading) return <div className="spinner" />;

  return (
    <div className="slide-up">
      <div className="topbar">
        <div>
          <div className="page-title">Faculty Dashboard</div>
          <div className="page-subtitle">Monitor student academic risk and update records faster</div>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {alerts.length > 0 && (
            <div className="tag danger" style={{ background: 'rgba(255,77,109,0.15)', color: 'var(--danger)', border: '1px solid rgba(255,77,109,0.3)' }}>
              Alerts: {alerts.length}
            </div>
          )}
          <div className="avatar">{user?.username?.slice(0, 2).toUpperCase()}</div>
        </div>
      </div>

      {activeTab === 'overview' && (
        <>
          <div className="stats-grid mb-4">
            <div className="stat-card" style={{ cursor: 'pointer' }} onClick={() => { setFilter('ALL'); navigate('/faculty/records'); }}>
              <div className="stat-icon">Students</div>
              <div className="stat-value">{students.length}</div>
              <div className="stat-label">Total Students</div>
            </div>
            <div className="stat-card danger pulse" style={{ cursor: 'pointer' }} onClick={() => { setFilter('HIGH'); navigate('/faculty/records'); }}>
              <div className="stat-icon">High</div>
              <div className="stat-value" style={{ color: 'var(--danger)' }}>{students.filter((s) => s.riskCategory === 'HIGH').length}</div>
              <div className="stat-label">High Risk</div>
            </div>
            <div className="stat-card warning" style={{ cursor: 'pointer' }} onClick={() => { setFilter('MEDIUM'); navigate('/faculty/records'); }}>
              <div className="stat-icon">Medium</div>
              <div className="stat-value" style={{ color: 'var(--warning-dark)' }}>{students.filter((s) => s.riskCategory === 'MEDIUM').length}</div>
              <div className="stat-label">Medium Risk</div>
            </div>
            <div className="stat-card success" style={{ cursor: 'pointer' }} onClick={() => { setFilter('LOW'); navigate('/faculty/records'); }}>
              <div className="stat-icon">Low</div>
              <div className="stat-value" style={{ color: 'var(--success)' }}>{students.filter((s) => s.riskCategory === 'LOW').length}</div>
              <div className="stat-label">Low Risk</div>
            </div>
          </div>

          <div className="card">
            <div className="section-title mb-2">Overall Class Performance</div>
            <div className="section-subtitle">A high-level view of marks and attendance across your students.</div>
            <div style={{ height: '300px', marginTop: '2rem' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={students}>
                  <XAxis dataKey="studentName" tick={{ fontSize: 10 }} interval={0} angle={-45} textAnchor="end" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="avgInternalMarks" name="Avg Marks" fill="var(--primary)" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="avgAttendance" name="Attendance" fill="var(--success)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}

      {(activeTab === 'records' || activeTab === 'interventions') && (
        <div className="card mb-4">
          <div className="section-header" style={{ alignItems: 'flex-start', gap: '1rem', flexWrap: 'wrap' }}>
            <div>
              <div className="section-title">Student Risk Overview</div>
              <div className="section-subtitle">Search by student name or roll number. Click a row to open records and add new data inline.</div>
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', width: '100%', justifyContent: 'space-between' }}>
              <div style={{ flex: '1 1 280px', maxWidth: '420px' }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Search student by name or roll no"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                {['ALL', 'HIGH', 'MEDIUM', 'LOW'].map((value) => (
                  <button key={value} className={`btn btn-sm ${filter === value ? 'btn-primary' : 'btn-ghost'}`} onClick={() => setFilter(value)}>
                    {value}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Roll No</th>
                  <th>Dept</th>
                  <th>Attendance</th>
                  <th>Marks</th>
                  <th>Risk</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((student) => {
                  const isExpanded = expandedStudentId === student.studentId;
                  const records = studentRecords[student.studentId] || [];

                  return (
                    <Fragment key={student.studentId}>
                      <tr
                        onClick={() => handleToggleStudent(student)}
                        style={{
                          cursor: 'pointer',
                          background: isExpanded ? 'rgba(108,99,255,0.08)' : '',
                        }}
                      >
                        <td style={{ fontWeight: 600 }}>{student.studentName}</td>
                        <td><span className="tag">{student.rollNumber || '—'}</span></td>
                        <td>{student.department}</td>
                        <td>
                          <div className="progress-bar-wrap">
                            <div className="progress-bar">
                              <div
                                className="progress-fill"
                                style={{
                                  width: `${student.avgAttendance || 0}%`,
                                  background: (student.avgAttendance || 0) < 75 ? 'var(--danger)' : 'var(--success)'
                                }}
                              />
                            </div>
                            <span className="progress-label">{student.avgAttendance ?? 0}%</span>
                          </div>
                        </td>
                        <td>{student.avgInternalMarks != null ? `${student.avgInternalMarks}/100` : 'N/A'}</td>
                        <td><span className={`risk-badge ${student.riskCategory || 'LOW'}`}>{student.riskCategory || 'LOW'}</span></td>
                        <td>
                          <button
                            className="btn btn-sm btn-primary"
                            onClick={(e) => {
                              e.stopPropagation();
                              setExpandedStudent(student);
                              setExpandedStudentId(student.studentId);
                              setShowModal(true);
                            }}
                          >
                            Intervene
                          </button>
                        </td>
                      </tr>
                      {isExpanded && (
                        <tr>
                          <td colSpan={7} style={{ padding: '0', background: 'rgba(108,99,255,0.03)' }}>
                            <div style={{ padding: '1rem 1rem 1.25rem' }}>
                              <div className="grid-2" style={{ alignItems: 'start' }}>
                                <div className="card" style={{ margin: 0 }}>
                                  <div className="section-title mb-2">{student.studentName}</div>
                                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
                                    <span className={`risk-badge ${student.riskCategory}`}>{student.riskCategory} Risk</span>
                                    <span className="tag">Score: {student.riskScore}</span>
                                    <span className="tag">Roll: {student.rollNumber || '—'}</span>
                                  </div>

                                  <ResponsiveContainer width="100%" height={220}>
                                    <RadarChart data={radarData}>
                                      <PolarGrid stroke="var(--border-light)" />
                                      <PolarAngleAxis dataKey="subject" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                                      <Radar dataKey="value" stroke="var(--primary)" fill="var(--primary)" fillOpacity={0.25} />
                                    </RadarChart>
                                  </ResponsiveContainer>

                                  <div style={{ padding: '0.75rem', background: 'var(--bg-surface)', borderRadius: 'var(--radius-md)', marginTop: '1rem', fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                                    <strong style={{ color: 'var(--primary-light)' }}>Recommendations</strong><br />
                                    {student.recommendations}
                                  </div>

                                  <button className="btn btn-primary btn-sm" style={{ marginTop: '1rem' }} onClick={() => setShowModal(true)}>
                                    Assign Intervention
                                  </button>
                                </div>

                                <div className="card" style={{ margin: 0 }}>
                                  <div className="section-title mb-2">Add Academic Record</div>
                                  <div className="grid-2">
                                    <div className="form-group">
                                      <label className="form-label">Semester</label>
                                      <input type="number" className="form-input" min={1} max={8} value={recordForm.semester} onChange={(e) => setRecordForm({ ...recordForm, semester: Number(e.target.value) })} />
                                    </div>
                                    <div className="form-group">
                                      <label className="form-label">Subject</label>
                                      <input type="text" className="form-input" placeholder="Data Structures" value={recordForm.subject} onChange={(e) => setRecordForm({ ...recordForm, subject: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                      <label className="form-label">Attendance (%)</label>
                                      <input type="number" className="form-input" min={0} max={100} value={recordForm.attendancePercentage} onChange={(e) => setRecordForm({ ...recordForm, attendancePercentage: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                      <label className="form-label">Internal Marks (%)</label>
                                      <input type="number" className="form-input" min={0} max={100} value={recordForm.internalMarks} onChange={(e) => setRecordForm({ ...recordForm, internalMarks: e.target.value })} />
                                    </div>
                                    {recordForm.semester > 1 && (
                                      <div className="form-group">
                                        <label className="form-label">Prev Sem Marks (%)</label>
                                        <input type="number" className="form-input" min={0} max={100} value={recordForm.previousSemesterMarks} onChange={(e) => setRecordForm({ ...recordForm, previousSemesterMarks: e.target.value })} />
                                      </div>
                                    )}
                                    <div className="form-group">
                                      <label className="form-label">Assignment Submissions (%)</label>
                                      <input type="number" className="form-input" min={0} max={100} value={recordForm.assignmentSubmissionPercentage} onChange={(e) => setRecordForm({ ...recordForm, assignmentSubmissionPercentage: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                      <label className="form-label">Late Submissions</label>
                                      <input type="number" className="form-input" min={0} value={recordForm.lateSubmissions} onChange={(e) => setRecordForm({ ...recordForm, lateSubmissions: e.target.value })} />
                                    </div>
                                    <div className="form-group">
                                      <label className="form-label">Absentee Patterns</label>
                                      <input type="number" className="form-input" min={0} value={recordForm.absenteePatterns} onChange={(e) => setRecordForm({ ...recordForm, absenteePatterns: e.target.value })} />
                                    </div>
                                  </div>
                                  <button className="btn btn-primary" onClick={handleAddRecord}>Add Record</button>
                                </div>
                              </div>

                              <div className="card" style={{ marginTop: '1rem' }}>
                                <div className="section-title mb-2">Academic Records</div>
                                {recordsLoading && !records.length ? (
                                  <div className="section-subtitle">Loading records...</div>
                                ) : records.length === 0 ? (
                                  <div className="section-subtitle">No academic records yet. Add one above and it will appear here immediately.</div>
                                ) : (
                                  <div className="data-table-wrap">
                                    <table className="data-table">
                                      <thead>
                                        <tr>
                                          <th>Semester</th>
                                          <th>Subject</th>
                                          <th>Attendance</th>
                                          <th>Marks</th>
                                          <th>Assignments</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        {records.map((record) => (
                                          <tr key={record.id}>
                                            <td>{record.semester}</td>
                                            <td>{record.subject}</td>
                                            <td>{record.attendancePercentage}%</td>
                                            <td>{record.internalMarks}</td>
                                            <td>{record.assignmentSubmissionPercentage}%</td>
                                          </tr>
                                        ))}
                                      </tbody>
                                    </table>
                                  </div>
                                )}
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                      No students found for this filter or search.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {alerts.length > 0 && (
        <div className="card">
          <div className="section-title mb-2">Unread System Alerts</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {alerts.slice(0, 5).map((alertItem) => (
              <div key={alertItem.id} style={{ padding: '0.75rem 1rem', background: 'rgba(255,77,109,0.07)', border: '1px solid rgba(255,77,109,0.2)', borderRadius: 'var(--radius-sm)', fontSize: '0.85rem' }}>
                {alertItem.message}
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  {new Date(alertItem.createdAt).toLocaleString()}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {showModal && expandedStudent && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal slide-up" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '400px' }}>
            <div className="modal-title">Assign Intervention</div>
            <div style={{ marginBottom: '1rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
              For: <strong style={{ color: 'var(--text-primary)' }}>{expandedStudent.studentName}</strong>
            </div>

            <div className="form-group">
              <label className="form-label">Intervention Type</label>
              <select className="form-input" value={intForm.type} onChange={(e) => setIntForm({ ...intForm, type: e.target.value })}>
                <option value="MENTORING_SESSION">Mentoring Session</option>
                <option value="TUTORING_REFERRAL">Tutoring Referral</option>
                <option value="WARNING_LETTER">Warning Letter</option>
                <option value="COUNSELING">Counseling</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Notes / Description</label>
              <textarea
                className="form-input"
                rows="4"
                placeholder="Details of the intervention..."
                value={intForm.description}
                onChange={(e) => setIntForm({ ...intForm, description: e.target.value })}
              />
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
              <button className="btn btn-ghost" onClick={() => setShowModal(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={assignIntervention}>Assign</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

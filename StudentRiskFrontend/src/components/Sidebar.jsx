import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navConfig = {
  ADMIN: [
    { to: '/admin', label: 'Overview', icon: '◈' },
    { to: '/admin/students', label: 'Student Records', icon: '◎' },
    { to: '/admin/faculty', label: 'Faculty Details', icon: '✦' },
    { to: '/admin/departments', label: 'Departments', icon: '▣' },
    { to: '/admin/reports', label: 'Analytics', icon: '◌' },
  ],
  FACULTY: [
    { to: '/faculty', label: 'Overview', icon: '◈' },
    { to: '/faculty/records', label: 'Academic Records', icon: '▤' },
    { to: '/faculty/interventions', label: 'Alerts', icon: '⚑' },
  ],
  STUDENT: [
    { to: '/student', label: 'Overview', icon: '◈' },
    { to: '/student', label: 'Ask Campus AI', icon: '✦' },
    { to: '/student/records', label: 'Resource Library', icon: '▣' },
    { to: '/student/records', label: 'Knowledge Galaxy', icon: '◎' },
    { to: '/student/records', label: 'PDF Studio', icon: '▤' },
    { to: '/student/records', label: 'Learning Planner', icon: '✓' },
    { to: '/student/records', label: 'Bookmarks', icon: '☆' },
    { to: '/student/alerts', label: 'Community', icon: '◍' },
    { to: '/student', label: 'Settings', icon: '⚙' },
  ],
};

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const links = navConfig[user?.role] || [];
  const initials = user?.username?.slice(0, 2).toUpperCase() || 'U';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-icon">✦</div>
        <div>
          <div className="logo-text">CampusHub</div>
          <div className="logo-sub">Your campus brain</div>
        </div>
      </div>

      <div className="nav-label">Navigate</div>
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          end
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <span className="nav-icon">{link.icon}</span>
          {link.label}
        </NavLink>
      ))}

      <div className="sidebar-footer">
        <div className="profile-strip">
          <div className="avatar">{initials}</div>
          <div>
            <div className="profile-name">{user?.username}</div>
            <div className="profile-role"><span className="tag purple">{user?.role}</span></div>
          </div>
        </div>
        <button className="nav-item" onClick={handleLogout}>
          <span className="nav-icon">🚪</span> Logout
        </button>
      </div>
    </aside>
  );
}

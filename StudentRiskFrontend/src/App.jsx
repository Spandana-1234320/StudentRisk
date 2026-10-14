import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminDashboard from './pages/AdminDashboard';
import FacultyDashboard from './pages/FacultyDashboard';
import StudentDashboard from './pages/StudentDashboard';
import Chatbot from './components/Chatbot';

function ProtectedLayout({ children, allowedRoles }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(user.role)) return <Navigate to="/login" replace />;
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">{children}</main>
    </div>
  );
}

function AppRoutes() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={`/${user.role.toLowerCase()}`} /> : <LoginPage />} />
      <Route path="/register" element={user ? <Navigate to={`/${user.role.toLowerCase()}`} /> : <RegisterPage />} />
      
      <Route path="/admin" element={<ProtectedLayout allowedRoles={['ADMIN']}><AdminDashboard /></ProtectedLayout>} />
      <Route path="/admin/*" element={<ProtectedLayout allowedRoles={['ADMIN']}><AdminDashboard /></ProtectedLayout>} />
      
      <Route path="/faculty" element={<ProtectedLayout allowedRoles={['FACULTY', 'ADMIN']}><FacultyDashboard /></ProtectedLayout>} />
      <Route path="/faculty/*" element={<ProtectedLayout allowedRoles={['FACULTY', 'ADMIN']}><FacultyDashboard /></ProtectedLayout>} />
      
      <Route path="/student" element={<ProtectedLayout allowedRoles={['STUDENT', 'FACULTY', 'ADMIN']}><StudentDashboard /></ProtectedLayout>} />
      <Route path="/student/*" element={<ProtectedLayout allowedRoles={['STUDENT', 'FACULTY', 'ADMIN']}><StudentDashboard /></ProtectedLayout>} />
      
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
        <Chatbot />
      </AuthProvider>
    </BrowserRouter>
  );
}

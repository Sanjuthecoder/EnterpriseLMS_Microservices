import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { TenantProvider } from './contexts/TenantContext';
import { ThemeProvider } from './contexts/ThemeContext';
import ErrorBoundary from './components/shared/ErrorBoundary';
import { ToastProvider } from './components/shared/ToastProvider';

// Layouts
import PublicLayout from './components/public/PublicLayout';

// Public Marketing Pages
import Home from './pages/public/Home';
import Features from './pages/public/Features';
import About from './pages/public/About';
import Contact from './pages/public/Contact';

// Dashboard / App Pages
import AuthPage from './pages/auth/AuthPage';
import SuperAdminDashboard from './pages/superAdmin/SuperAdminDashboard';
import CompanyAdminDashboard from './pages/companyAdmin/CompanyAdminDashboard';
import EmployeeDashboard from './pages/employee/EmployeeDashboard';
import CoursePlayer from './pages/employee/CoursePlayer';
import Assessment from './pages/employee/Assessment';
import UpliftResults from './pages/employee/UpliftResults';
import CreatorDashboard from './pages/creator/CreatorDashboard';

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <TenantProvider>
          <ThemeProvider>
            <ToastProvider>
              <BrowserRouter>
              <Routes>
                {/* Public Marketing Website */}
                <Route element={<PublicLayout />}>
                  <Route path="/" element={<Home />} />
                  <Route path="/features" element={<Features />} />
                  <Route path="/about" element={<About />} />
                  <Route path="/contact" element={<Contact />} />
                </Route>

                {/* Public Auth Portal */}
                <Route path="/auth" element={<AuthPage />} />
                
                {/* Super Admin Dashboard */}
                <Route path="/super-admin/*" element={<SuperAdminDashboard />} />

                {/* Company Admin Dashboard */}
                <Route path="/company-admin/*" element={<CompanyAdminDashboard />} />

                {/* Employee / Learner Routes */}
                <Route path="/employee/*" element={<EmployeeDashboard />} />
                <Route path="/employee/courses/:courseId/player" element={<CoursePlayer />} />
                <Route path="/employee/courses/:courseId/pre-quiz" element={<Assessment />} />
                <Route path="/employee/courses/:courseId/post-quiz" element={<Assessment />} />
                <Route path="/employee/courses/:courseId/uplift" element={<UpliftResults />} />

                {/* Content Creator Routes */}
                <Route path="/creator/*" element={<CreatorDashboard />} />

                {/* Default Redirect */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
              </BrowserRouter>
            </ToastProvider>
          </ThemeProvider>
        </TenantProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;

import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from '../../components/shared/Navbar';
import Sidebar from '../../components/shared/Sidebar';
import { LayoutDashboard, Users, Settings, BookOpen, BarChart2, User, Target } from 'lucide-react';

// Tabs
import OverviewTab from './OverviewTab';
import EmployeeManagement from './EmployeeManagement';
import ThemeCustomizer from './ThemeCustomizer';
import CourseAssignment from './CourseAssignment';
import AnalyticsOverview from './AnalyticsOverview';

import CertificatesTab from './CertificatesTab';
import CompanyAdminProfile from './CompanyAdminProfile';

const CompanyAdminDashboard = () => {
  const sidebarLinks = [
    { label: 'My Admin Profile', path: '/company-admin/profile', icon: <User size={18} /> },
    { label: 'Overview', path: '/company-admin', icon: <LayoutDashboard size={18} /> },
    { label: 'Staff Management', path: '/company-admin/staff', icon: <Users size={18} /> },
    { label: 'Course Assignment', path: '/company-admin/assignments', icon: <BookOpen size={18} /> },
    { label: 'Analytics & ROI', path: '/company-admin/analytics', icon: <BarChart2 size={18} /> },
    { label: 'Certificates', path: '/company-admin/certificates', icon: <Target size={18} /> },
    { label: 'Branding', path: '/company-admin/branding', icon: <Settings size={18} /> }
  ];

  return (
    <div>
      <Navbar roleTitle="Company Admin" />
      <div style={{ display: 'flex' }}>
        <Sidebar links={sidebarLinks} />
        <main style={{ flexGrow: 1, padding: '2rem', background: 'var(--bg-primary)', minHeight: 'calc(100vh - 73px)' }}>
          <Routes>
            <Route path="/" element={<OverviewTab />} />
            <Route path="/profile" element={<CompanyAdminProfile />} />
            <Route path="/staff" element={<EmployeeManagement />} />
            <Route path="/assignments" element={<CourseAssignment />} />
            <Route path="/analytics" element={<AnalyticsOverview />} />
            <Route path="/certificates" element={<CertificatesTab />} />
            <Route path="/branding" element={<ThemeCustomizer />} />
            <Route path="*" element={<Navigate to="/company-admin" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
};

export default CompanyAdminDashboard;

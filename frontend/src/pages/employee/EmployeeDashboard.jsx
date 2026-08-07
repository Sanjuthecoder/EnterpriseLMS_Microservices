import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from '../../components/shared/Navbar';
import Sidebar from '../../components/shared/Sidebar';
import { BookOpen, Award, User } from 'lucide-react';

// Tabs
import OverviewTab from './OverviewTab';
import CertificatesTab from './CertificatesTab';

import EmployeeProfile from './EmployeeProfile';

const EmployeeDashboard = () => {
  const sidebarLinks = [
    { label: 'My Profile', path: '/employee/profile', icon: <User size={18} /> },
    { label: 'My Courses', path: '/employee', icon: <BookOpen size={18} /> },
    { label: 'My Certificates', path: '/employee/certificates', icon: <Award size={18} /> }
  ];

  return (
    <div>
      <Navbar roleTitle="Employee Learner" />
      <div className="flex">
        <Sidebar links={sidebarLinks} />
        <main style={{ flexGrow: 1, padding: '2rem', background: 'var(--bg-primary)', minHeight: 'calc(100vh - 73px)' }}>
          <Routes>
            <Route path="/" element={<OverviewTab />} />
            <Route path="/profile" element={<EmployeeProfile />} />
            <Route path="/certificates" element={<CertificatesTab />} />
            <Route path="*" element={<Navigate to="/employee" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
};

export default EmployeeDashboard;

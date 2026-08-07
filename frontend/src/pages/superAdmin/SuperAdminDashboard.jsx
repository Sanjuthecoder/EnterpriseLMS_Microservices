import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Navbar from '../../components/shared/Navbar';
import Sidebar from '../../components/shared/Sidebar';
import { LayoutDashboard, Building2, ShieldCheck, BarChart2, User } from 'lucide-react';

// Tabs
import OverviewTab from './OverviewTab';
import OrganizationsTab from './OrganizationsTab';
import ApprovalsTab from './ApprovalsTab';
import AnalyticsTab from './AnalyticsTab';
import CourseDistributionTab from './CourseDistributionTab';

import SuperAdminProfile from './SuperAdminProfile';

const SuperAdminDashboard = () => {
  const location = useLocation();

  const sidebarLinks = [
    { label: 'System Profile', path: '/super-admin/profile', icon: <User size={18} /> },
    { label: 'Overview', path: '/super-admin', icon: <LayoutDashboard size={18} /> },
    { label: 'Organizations', path: '/super-admin/organizations', icon: <Building2 size={18} /> },
    { label: 'Approvals', path: '/super-admin/approvals', icon: <ShieldCheck size={18} /> },
    { label: 'Distribution', path: '/super-admin/distribution', icon: <Building2 size={18} /> },
    { label: 'Analytics', path: '/super-admin/analytics', icon: <BarChart2 size={18} /> }
  ];

  return (
    <div>
      <Navbar roleTitle="Super Admin" />
      <div style={{ display: 'flex' }}>
        <Sidebar links={sidebarLinks} />
        <main style={{ flexGrow: 1, padding: '2rem', background: 'var(--bg-primary)', minHeight: 'calc(100vh - 73px)' }}>
          <Routes>
            <Route path="/" element={<OverviewTab />} />
            <Route path="/profile" element={<SuperAdminProfile />} />
            <Route path="/organizations" element={<OrganizationsTab />} />
            <Route path="/approvals" element={<ApprovalsTab />} />
            <Route path="/distribution" element={<CourseDistributionTab />} />
            <Route path="/analytics" element={<AnalyticsTab />} />
            <Route path="*" element={<Navigate to="/super-admin" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
};

export default SuperAdminDashboard;

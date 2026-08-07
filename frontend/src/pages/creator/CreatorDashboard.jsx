import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from '../../components/shared/Navbar';
import Sidebar from '../../components/shared/Sidebar';
import { LayoutDashboard, Edit3, FileQuestion, LineChart, User } from 'lucide-react';

// Tabs
import OverviewTab from './OverviewTab';
import CourseBuilder from './CourseBuilder';
import QuizBuilder from './QuizBuilder';
import AnalyticsTab from './AnalyticsTab';

import CreatorProfile from './CreatorProfile';

const CreatorDashboard = () => {
  const sidebarLinks = [
    { label: 'My Portfolio', path: '/creator/profile', icon: <User size={18} /> },
    { label: 'Overview', path: '/creator', icon: <LayoutDashboard size={18} /> },
    { label: 'Course Builder', path: '/creator/courses', icon: <Edit3 size={18} /> },
    { label: 'Assessment Architect', path: '/creator/quizzes', icon: <FileQuestion size={18} /> },
    { label: 'Instructional Insights', path: '/creator/insights', icon: <LineChart size={18} /> }
  ];

  return (
    <div>
      <Navbar roleTitle="Content Creator" />
      <div className="flex">
        <Sidebar links={sidebarLinks} />
        <main style={{ flexGrow: 1, padding: '2rem', background: 'var(--bg-primary)', minHeight: 'calc(100vh - 73px)' }}>
          <Routes>
            <Route path="/" element={<OverviewTab />} />
            <Route path="/profile" element={<CreatorProfile />} />
            <Route path="/courses" element={<CourseBuilder />} />
            <Route path="/quizzes" element={<QuizBuilder />} />
            <Route path="/insights" element={<AnalyticsTab />} />
            <Route path="*" element={<Navigate to="/creator" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
};

export default CreatorDashboard;

import React, { useState, useEffect } from 'react';
import { Users, Building2, BookOpen, Activity, ArrowUpRight, ShieldCheck } from 'lucide-react';
import api from '../../services/api';

const OverviewTab = () => {
  const [metrics, setMetrics] = useState({
    totalOrganizations: 0,
    activeOrganizations: 0,
    totalUsers: 0,
    totalEmployees: 0,
    totalCourses: 0,
    totalCompanies: 0
  });
  const [loading, setLoading] = useState(true);
  const [pendingCoursesCount, setPendingCoursesCount] = useState(0);

  useEffect(() => {
    const fetchOverviewData = async () => {
      try {
        const metricsRes = await api.get('/super-admin/dashboard/metrics');
        const allCoursesRes = await api.get('/super-admin/courses');
        
        const totalCourses = allCoursesRes.data.filter(c => c.status === 'PUBLISHED').length;
        
        setMetrics({
          ...metricsRes.data,
          totalCourses: totalCourses
        });

        const pendingCoursesRes = await api.get('/super-admin/courses/pending');
        setPendingCoursesCount(pendingCoursesRes.data.length);
      } catch (err) {
        console.error('Failed to fetch dashboard metrics', err);
      } finally {
        setLoading(false);
      }
    };
    fetchOverviewData();
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '200px', color: 'var(--text-secondary)' }}>
        Loading platform overview metrics...
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        Platform Overview
      </h2>
      
      {/* Quick Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.5rem', marginBottom: '2.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Total Organizations</div>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--primary-color)' }}>
              <Building2 size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>
            {metrics.totalOrganizations}
          </div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            {metrics.activeOrganizations} active tenants
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Active Users</div>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--success-color)' }}>
              <Users size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>
            {metrics.totalUsers}
          </div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            {metrics.totalEmployees} registered employees
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Published Courses</div>
            <div style={{ background: 'rgba(245, 158, 11, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--warning-color)' }}>
              <BookOpen size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>
            {metrics.totalCourses}
          </div>
          <div style={{ color: 'var(--warning-color)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            {pendingCoursesCount} pending approval
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Registered Companies</div>
            <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '8px', borderRadius: '8px', color: '#8b5cf6' }}>
              <Activity size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>
            {metrics.totalCompanies}
          </div>
          <div style={{ color: 'var(--success-color)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            Active Tenants
          </div>
        </div>
      </div>

      {/* Engagement Trends & System Alerts */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)' }}>Engagement Trends</h3>
          <div style={{ 
            height: '250px', 
            background: 'linear-gradient(to top, rgba(37, 99, 235, 0.05), transparent)',
            border: '1px dashed var(--border-color)',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--text-secondary)'
          }}>
            [Chart Area: Weekly Active Sessions]
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)' }}>System Alerts</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {pendingCoursesCount > 0 && (
              <div style={{ padding: '1rem', background: 'rgba(245, 158, 11, 0.1)', borderLeft: '3px solid var(--warning-color)', borderRadius: '4px' }}>
                <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#b45309' }}>{pendingCoursesCount} Course Approvals Pending</h4>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Please review the approvals queue.</p>
              </div>
            )}
            <div style={{ padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', borderLeft: '3px solid var(--success-color)', borderRadius: '4px' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#047857' }}>Database Backup Complete</h4>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Last run: 10 mins ago.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OverviewTab;

import React, { useState, useEffect } from 'react';
import { Users, BookOpen, Target, TrendingUp, AlertTriangle } from 'lucide-react';
import api from '../../services/api';
import PremiumSubscriptionModal from './PremiumSubscriptionModal';
import { useToast } from '../../components/shared/ToastProvider';

const OverviewTab = () => {
  const toast = useToast();
  const [employees, setEmployees] = useState([]);
  const [courses, setCourses] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Premium state
  const [isPremiumModalOpen, setIsPremiumModalOpen] = useState(false);
  const [isPremium, setIsPremium] = useState(false); // In a real app, this comes from user context/company details

  useEffect(() => {
    const fetchData = async () => {
      try {
        const userDetails = JSON.parse(localStorage.getItem('user')) || {};
        const companyId = userDetails.companyId || 1;
        const [empRes, coursesRes, enrollmentsRes, analyticsRes] = await Promise.all([
          api.get('/company-admin/employees'),
          api.get('/company-admin/courses'),
          api.get('/company-admin/enrollments'),
          api.get('/company-admin/analytics/roi')
        ]);
        setEmployees(empRes.data || []);
        setCourses(coursesRes.data || []);
        setEnrollments(enrollmentsRes.data || []);
        setAnalytics(analyticsRes.data || null);
        
        
        setIsPremium(userDetails.subscriptionTier === 'PREMIUM');
      } catch (err) {
        console.error('Error fetching dashboard overview data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return <div style={{ padding: '2rem', color: 'var(--text-secondary)' }}>Loading dashboard overview...</div>;
  }

  const activeEmployeesCount = employees.filter(e => e.status !== 'PENDING').length;
  const pendingEmployeesCount = employees.filter(e => e.status === 'PENDING').length;
  const availableCoursesCount = courses.length;
  
  const avgTimeToCompetency = analytics?.averageTimeToCompetencyDays 
    ? `${analytics.averageTimeToCompetencyDays.toFixed(1)}d` 
    : '0.0d';
    
  const avgCompletionRate = analytics?.averageCompletionRate 
    ? `${analytics.averageCompletionRate.toFixed(0)}%` 
    : '0%';

  // Calculate overdue enrollments (deadline passed and progress < 100)
  const today = new Date();
  const overdueEnrollments = enrollments.filter(e => {
    if (e.status === 'COMPLETED' || e.progressPercentage === 100) return false;
    if (!e.deadline) return false;
    return new Date(e.deadline) < today;
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
          Company Overview
        </h2>
        {!isPremium ? (
          <button 
            onClick={() => setIsPremiumModalOpen(true)}
            style={{ padding: '0.5rem 1rem', background: 'linear-gradient(135deg, #F59E0B, #EA580C)', color: 'white', border: 'none', borderRadius: '8px', fontWeight: 600, cursor: 'pointer', boxShadow: '0 4px 6px -1px rgba(245, 158, 11, 0.4)' }}
          >
            Upgrade to Premium
          </button>
        ) : (
          <div style={{ padding: '0.5rem 1rem', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--success-color)', borderRadius: '8px', fontWeight: 600, border: '1px solid rgba(16, 185, 129, 0.2)' }}>
            Premium Member
          </div>
        )}
      </div>

      <PremiumSubscriptionModal 
        isOpen={isPremiumModalOpen} 
        onClose={() => setIsPremiumModalOpen(false)} 
        companyId={JSON.parse(localStorage.getItem('user'))?.companyId || 1}
        onUpgradeSuccess={() => {
          setIsPremium(true);
          toast.premium('Payment successful! You are now a Premium Member. Please login again to enjoy premium features.');
        }}
      />
      
      {/* Quick Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.5rem', marginBottom: '2.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Active Employees</div>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--primary-color)' }}>
              <Users size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{activeEmployeesCount}</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            {pendingEmployeesCount} pending approval
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Available Courses</div>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--success-color)' }}>
              <BookOpen size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{availableCoursesCount}</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            Mapped by Super Admin
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Avg Time to Competency</div>
            <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '8px', borderRadius: '8px', color: '#8b5cf6' }}>
              <Target size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{avgTimeToCompetency}</div>
          <div style={{ color: 'var(--success-color)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            Active monitoring enabled
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Average Completion</div>
            <div style={{ background: 'rgba(245, 158, 11, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--warning-color)' }}>
              <TrendingUp size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{avgCompletionRate}</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', fontWeight: 500 }}>
            Across all course enrollments
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
        {/* Recent Enrollments */}
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)' }}>Recent Training Activity</h3>
          <div style={{ overflowX: 'auto' }}>
            {enrollments.length === 0 ? (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                No active course assignments or student progress recorded yet.
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ background: 'rgba(0,0,0,0.02)', color: 'var(--text-secondary)', fontSize: '0.85rem', textTransform: 'uppercase' }}>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Employee</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Course</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Progress</th>
                    <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Deadline</th>
                  </tr>
                </thead>
                <tbody>
                  {enrollments.slice(0, 5).map((enrollment, idx) => {
                    const emp = employees.find(e => String(e.userId) === String(enrollment.employeeId));
                    const crs = courses.find(c => String(c.courseId) === String(enrollment.courseId));
                    const progress = enrollment.progressPercentage || 0;
                    
                    return (
                      <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.95rem' }}>
                        <td style={{ padding: '1rem', fontWeight: 500 }}>{emp ? emp.username : `User ${enrollment.employeeId}`}</td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{crs ? crs.title : `Course #${enrollment.courseId}`}</td>
                        <td style={{ padding: '1rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{progress}%</span>
                            <div style={{ width: '50px', height: '4px', background: 'var(--border-color)', borderRadius: '2px', overflow: 'hidden' }}>
                              <div style={{ width: `${progress}%`, height: '100%', background: progress === 100 ? 'var(--success-color)' : 'var(--primary-color)' }} />
                            </div>
                          </div>
                        </td>
                        <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
                          {enrollment.deadline ? new Date(enrollment.deadline).toLocaleDateString(undefined, {month: 'short', day: 'numeric'}) : 'No limit'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Action Items */}
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)' }}>Action Items</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ padding: '1rem', background: overdueEnrollments.length > 0 ? 'rgba(239, 68, 68, 0.05)' : 'rgba(16, 185, 129, 0.05)', border: overdueEnrollments.length > 0 ? '1px solid rgba(239, 68, 68, 0.2)' : '1px solid rgba(16, 185, 129, 0.2)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: overdueEnrollments.length > 0 ? 'var(--danger-color)' : 'var(--success-color)', fontWeight: 600, marginBottom: '0.25rem' }}>
                <AlertTriangle size={16} /> {overdueEnrollments.length} Overdue Enrollments
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                {overdueEnrollments.length > 0 
                  ? 'Follow up with employees who missed their course deadlines.' 
                  : 'All employees are within their assignment deadlines.'}
              </p>
            </div>
            
            <div style={{ padding: '1rem', background: pendingEmployeesCount > 0 ? 'rgba(245, 158, 11, 0.05)' : 'rgba(16, 185, 129, 0.05)', border: pendingEmployeesCount > 0 ? '1px solid rgba(245, 158, 11, 0.2)' : '1px solid rgba(16, 185, 129, 0.2)', borderRadius: 'var(--radius-md)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: pendingEmployeesCount > 0 ? 'var(--warning-color)' : 'var(--success-color)', fontWeight: 600, marginBottom: '0.25rem' }}>
                <Users size={16} /> {pendingEmployeesCount} Pending Approvals
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                {pendingEmployeesCount > 0 
                  ? 'New self-registered employees waiting for review.' 
                  : 'No pending employee registrations.'}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OverviewTab;

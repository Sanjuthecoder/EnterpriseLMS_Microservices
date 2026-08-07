import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Clock, CheckCircle } from 'lucide-react';
import api from '../../services/api';

const OverviewTab = () => {
  const navigate = useNavigate();
  const [assignedCourses, setAssignedCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await api.get('/employees/dashboard');
        setAssignedCourses(response.data);
      } catch (err) {
        console.error('Failed to load employee dashboard:', err);
        if (err.response?.status === 403) {
          setError('Access Denied: Your session may have expired or your account lacks the required permissions. Please log out and log back in.');
        } else if (err.response?.status === 401) {
          setError('Your session has expired. Please log in again.');
        } else {
          setError('Could not load your learning dashboard. Please try again later.');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) return <div style={{ padding: '2rem' }}>Loading dashboard...</div>;
  if (error) return <div style={{ padding: '2rem', color: 'var(--danger-color)' }}>{error}</div>;

  return (
    <div>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        My Learning Dashboard
      </h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Complete your required training. Taking the Pre-Quiz will dynamically adapt the course to skip material you already know.
      </p>
      
      {assignedCourses.length === 0 ? (
        <div className="glass-panel" style={{ padding: '4rem 2rem', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem' }}>
            <Clock size={40} />
          </div>
          <h3 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>You're all caught up!</h3>
          <p style={{ color: 'var(--text-secondary)', maxWidth: '400px', margin: '0 auto' }}>
            There are no courses assigned to you at the moment. Take a break, or check back later when new training is available.
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
          {assignedCourses.map((course) => (
            <div key={course.enrollmentId} className="glass-panel" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>{course.courseTitle}</h3>
                {course.status === 'COMPLETED' ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem', color: 'var(--success-color)', fontWeight: 700 }}>
                    <CheckCircle size={14} /> COMPLETED
                  </div>
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem', color: 'var(--danger-color)', fontWeight: 600 }}>
                    <Clock size={14} /> Due: {course.deadline ? new Date(course.deadline).toLocaleDateString() : 'N/A'}
                  </div>
                )}
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '0.25rem', color: 'var(--text-secondary)' }}>
                  <span>Course Progress</span>
                  <span>{course.progressPercentage || 0}%</span>
                </div>
                <div style={{ height: '8px', background: 'var(--border-color)', borderRadius: '4px', overflow: 'hidden' }}>
                  <div style={{ width: `${course.progressPercentage || 0}%`, height: '100%', background: course.status === 'COMPLETED' ? 'var(--success-color)' : 'var(--primary-color)' }} />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
                {course.status === 'ASSIGNED' && (
                  <button 
                    onClick={() => navigate(course.lessonGatingMap ? `/employee/courses/${course.courseId}/player` : `/employee/courses/${course.courseId}/pre-quiz`)} 
                    className="btn btn-primary"
                    style={{ width: '100%' }}
                  >
                    {course.lessonGatingMap ? 'Continue Learning' : 'Start Diagnostic (Pre-Quiz)'}
                  </button>
                )}
                {course.status === 'IN_PROGRESS' && (
                  <>
                    <button 
                      onClick={() => navigate(`/employee/courses/${course.courseId}/player`)} 
                      className="btn btn-primary"
                      style={{ flexGrow: 1 }}
                    >
                      Resume Learning
                    </button>
                    {/* Optionally, if the user completed all recommended lessons, show post-quiz button */}
                    {course.progressPercentage === 100 && (
                      <button 
                        onClick={() => navigate(`/employee/courses/${course.courseId}/post-quiz`)} 
                        className="btn btn-secondary"
                      >
                        Take Final Assessment
                      </button>
                    )}
                  </>
                )}
                {course.status === 'COMPLETED' && (
                  <>
                    <button 
                      onClick={() => navigate(`/employee/courses/${course.courseId}/uplift`)} 
                      className="btn btn-secondary"
                      style={{ flexGrow: 1 }}
                    >
                      View Skill Uplift Report
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default OverviewTab;

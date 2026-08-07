import React, { useState, useEffect } from 'react';
import { BookOpen, Video, Users, Edit3, X, Plus } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const OverviewTab = () => {
  const { user } = useAuth();
  const [courses, setCourses] = useState([]);
  const [stats, setStats] = useState({ activeLearners: 0, videoLessons: 0 });
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [toast, setToast] = useState(null);
  const [creating, setCreating] = useState(false);
  const [newCourse, setNewCourse] = useState({
    title: '',
    description: '',
    category: '',
    version: 1,
    creatorId: user?.userId
  });

  const fetchCourses = async () => {
    if (!user || !user.userId) return;
    try {
      const response = await api.get(`/creator/courses?creatorId=${user.userId}`);
      setCourses(response.data || []);
    } catch (err) {
      console.error('Failed to fetch creator courses', err);
    }

    try {
      const statsResponse = await api.get(`/creator/stats?creatorId=${user.userId}`);
      setStats(statsResponse.data || { activeLearners: 0, videoLessons: 0 });
    } catch (err) {
      console.error('Failed to fetch creator stats', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCourses();
  }, [user]);

  const handleCreateCourse = async (e) => {
    e.preventDefault();
    if (!newCourse.title.trim()) {
      setToast({ message: 'Course title is required.', type: 'error' });
      return;
    }
    setCreating(true);
    try {
      const payload = { ...newCourse, creatorId: user.userId, status: 'DRAFT' };
      await api.post('/creator/courses', payload);
      setToast({ message: `"${newCourse.title}" created successfully!`, type: 'success' });
      setShowModal(false);
      setNewCourse({ title: '', description: '', category: '', version: 1, creatorId: user?.userId });
      fetchCourses();
    } catch (err) {
      console.error('Failed to create course', err);
      setToast({ message: 'Failed to create course.', type: 'error' });
    } finally {
      setCreating(false);
    }
  };

  if (loading) return <div style={{ padding: '2rem', color: 'var(--text-secondary)' }}>Loading dashboard...</div>;

  return (
    <div>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        Creator Dashboard
      </h2>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.5rem', marginBottom: '2.5rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Authored Courses</div>
            <div style={{ background: 'rgba(37, 99, 235, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--primary-color)' }}>
              <BookOpen size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{courses.length}</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 500 }}>
            {courses.filter(c => c.status === 'PUBLISHED').length} Published, {courses.filter(c => c.status !== 'PUBLISHED').length} Draft/Pending
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Total Active Learners</div>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', padding: '8px', borderRadius: '8px', color: 'var(--success-color)' }}>
              <Users size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{stats.activeLearners}</div>
          <div style={{ color: 'var(--success-color)', fontSize: '0.85rem', fontWeight: 500 }}>Currently enrolled & active</div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase' }}>Video Lessons Hosted</div>
            <div style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '8px', borderRadius: '8px', color: '#8b5cf6' }}>
              <Video size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontWeight: 700, margin: '0.5rem 0', color: 'var(--text-primary)' }}>{stats.videoLessons}</div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 500 }}>Stored in Local Media System</div>
        </div>
      </div>

      {/* Course Table */}
      <div className="glass-panel" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)' }}>My Courses</h3>
          <button
            id="create-course-btn"
            className="btn btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            onClick={() => setShowModal(true)}
          >
            <Edit3 size={16} /> Create New Course
          </button>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'rgba(0,0,0,0.02)', color: 'var(--text-secondary)', fontSize: '0.85rem', textTransform: 'uppercase' }}>
                <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Course Title</th>
                <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Version</th>
                <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Category</th>
                <th style={{ padding: '1rem 1.5rem', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {courses.map(course => (
                <tr key={course.courseId} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.95rem' }}>
                  <td style={{ padding: '1rem 1.5rem', fontWeight: 600, color: 'var(--text-primary)' }}>{course.title}</td>
                  <td style={{ padding: '1rem 1.5rem' }}>
                    <span style={{ fontFamily: 'monospace', background: '#f1f5f9', padding: '2px 8px', borderRadius: '4px', fontSize: '0.85rem' }}>v{course.version}</span>
                  </td>
                  <td style={{ padding: '1rem 1.5rem' }}>
                    <span style={{
                      fontSize: '0.75rem',
                      background: course.status === 'PUBLISHED' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(245, 158, 11, 0.1)',
                      color: course.status === 'PUBLISHED' ? 'var(--success-color)' : 'var(--warning-color)',
                      padding: '4px 10px', borderRadius: '12px', fontWeight: 700
                    }}>{course.status.replace(/_/g, ' ')}</span>
                  </td>
                  <td style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)' }}>{course.category || '—'}</td>
                  <td style={{ padding: '1rem 1.5rem', textAlign: 'right' }}>
                    {course.status === 'PUBLISHED' ? (
                      <button className="btn btn-secondary" style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }}>Clone to v2</button>
                    ) : (
                      <button className="btn btn-primary" style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }}>Resume Editing</button>
                    )}
                  </td>
                </tr>
              ))}
              {courses.length === 0 && (
                <tr>
                  <td colSpan="5" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No courses yet. Click <strong>Create New Course</strong> to get started!
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '1rem', fontStyle: 'italic' }}>
          * Immutable Versioning: Editing a published course automatically creates a V2 draft. Active learners finish V1 uninterrupted.
        </p>
      </div>

      {/* Create Course Modal */}
      {showModal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '520px', padding: '2rem', position: 'relative' }}>
            <button
              onClick={() => setShowModal(false)}
              style={{ position: 'absolute', top: '1rem', right: '1rem', background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
            >
              <X size={20} />
            </button>

            <h3 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.25rem', color: 'var(--text-primary)' }}>
              Create New Course
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
              Set up your course shell. You can add lessons and quizzes from the Course Builder.
            </p>

            <form onSubmit={handleCreateCourse}>
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Course Title <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                <input
                  id="course-title-input"
                  type="text"
                  className="form-input"
                  placeholder="e.g. Cybersecurity Fundamentals"
                  value={newCourse.title}
                  onChange={(e) => setNewCourse({ ...newCourse, title: e.target.value })}
                  required
                  autoFocus
                />
              </div>

              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Category <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                <input
                  type="text"
                  className="form-input"
                  required
                  placeholder="e.g. Security, Engineering, Leadership"
                  value={newCourse.category}
                  onChange={(e) => setNewCourse({ ...newCourse, category: e.target.value })}
                />
              </div>

              <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                <label className="form-label">Description <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                <textarea
                  className="form-input"
                  required
                  rows="3"
                  placeholder="Brief overview of what this course covers..."
                  value={newCourse.description}
                  onChange={(e) => setNewCourse({ ...newCourse, description: e.target.value })}
                />
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={creating}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                >
                  <Plus size={16} /> {creating ? 'Creating...' : 'Create Course'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default OverviewTab;

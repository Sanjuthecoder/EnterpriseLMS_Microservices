import React, { useState, useEffect } from 'react';
import { CheckCircle, XCircle, Clock, RefreshCw } from 'lucide-react';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const ApprovalsTab = () => {
  const [pendingOrgs, setPendingOrgs] = useState([]);
  const [pendingCourses, setPendingCourses] = useState([]);
  const [pendingCreators, setPendingCreators] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState({});
  const [toast, setToast] = useState(null);

  const fetchApprovals = async () => {
    setLoading(true);
    try {
      const orgsRes = await api.get('/super-admin/organizations?size=100');
      if (orgsRes.data && orgsRes.data.content) {
        const pending = orgsRes.data.content.filter(o => o.status === 'PENDING');
        setPendingOrgs(pending);
      } else {
        setPendingOrgs([]);
      }

      const coursesRes = await api.get('/super-admin/courses/pending');
      setPendingCourses(coursesRes.data || []);

      const creatorsRes = await api.get('/super-admin/creators');
      if (creatorsRes.data) {
        const pending = creatorsRes.data.filter(c => c.status === 'PENDING');
        setPendingCreators(pending);
      } else {
        setPendingCreators([]);
      }
    } catch (err) {
      console.error('Failed to fetch approvals queue', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApprovals();
  }, []);

  const handleApproveOrg = async (orgId) => {
    setProcessing(prev => ({ ...prev, [`org_${orgId}`]: 'approving' }));
    try {
      await api.patch(`/super-admin/organizations/${orgId}/approve`);
      setToast({ message: 'Organization approved and provisioned successfully!', type: 'success' });
      fetchApprovals();
    } catch (err) {
      setToast({ message: 'Failed to approve organization.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[`org_${orgId}`];
        return next;
      });
    }
  };

  const handleDeactivateOrg = async (orgId) => {
    if (!window.confirm('Are you sure you want to reject and deactivate this organization request?')) return;
    setProcessing(prev => ({ ...prev, [`org_${orgId}`]: 'rejecting' }));
    try {
      await api.delete(`/super-admin/organizations/${orgId}`);
      setToast({ message: 'Organization registration rejected.', type: 'success' });
      fetchApprovals();
    } catch (err) {
      setToast({ message: 'Failed to reject organization.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[`org_${orgId}`];
        return next;
      });
    }
  };

  const handleApproveCreator = async (creatorId) => {
    setProcessing(prev => ({ ...prev, [`creator_${creatorId}`]: 'approving' }));
    try {
      await api.patch(`/super-admin/creators/${creatorId}/approve`);
      setToast({ message: 'Creator approved successfully!', type: 'success' });
      fetchApprovals();
    } catch (err) {
      setToast({ message: 'Failed to approve creator.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[`creator_${creatorId}`];
        return next;
      });
    }
  };

  const handleRejectCreator = async (creatorId) => {
    if (!window.confirm('Are you sure you want to reject this creator?')) return;
    setProcessing(prev => ({ ...prev, [`creator_${creatorId}`]: 'rejecting' }));
    try {
      await api.patch(`/super-admin/creators/${creatorId}/reject`);
      setToast({ message: 'Creator registration rejected.', type: 'success' });
      fetchApprovals();
    } catch (err) {
      setToast({ message: 'Reject not implemented on backend yet — creator remains pending.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[`creator_${creatorId}`];
        return next;
      });
    }
  };

  const handleApproveCourse = async (courseId) => {
    setProcessing(prev => ({ ...prev, [`course_${courseId}`]: 'approving' }));
    try {
      await api.patch(`/super-admin/courses/${courseId}/approve`);
      setToast({ message: 'Course version approved and published!', type: 'success' });
      fetchApprovals();
    } catch (err) {
      setToast({ message: 'Failed to approve course.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[`course_${courseId}`];
        return next;
      });
    }
  };

  const handleRejectCourse = async (courseId) => {
    setProcessing(prev => ({ ...prev, [`course_${courseId}`]: 'rejecting' }));
    try {
      await api.patch(`/super-admin/courses/${courseId}/reject`);
      setToast({ message: 'Course version rejected and returned to draft.', type: 'success' });
      fetchApprovals();
    } catch (err) {
      setToast({ message: 'Failed to reject course.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[`course_${courseId}`];
        return next;
      });
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Approval Workflows
        </h2>
        <button 
          onClick={fetchApprovals} 
          className="btn btn-secondary" 
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          title="Refresh queue"
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading pending approvals queue...
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '2rem' }}>
          
          {/* Organization Approvals */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>New Organization Signups</h3>
              <span style={{ background: 'rgba(245, 158, 11, 0.1)', color: 'var(--warning-color)', padding: '2px 8px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 700 }}>
                {pendingOrgs.length} Pending
              </span>
            </div>
            
            <div>
              {pendingOrgs.map(org => (
                <div key={org.orgId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', marginBottom: '1rem', background: 'var(--bg-secondary)' }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--text-primary)' }}>{org.name}</div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.25rem' }}>
                      <span>Primary contact: {org.email}</span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={14} /> Created: {new Date(org.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button 
                      onClick={() => handleDeactivateOrg(org.orgId)}
                      disabled={processing[`org_${org.orgId}`]}
                      className="btn btn-secondary" 
                      style={{ color: 'var(--danger-color)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                    >
                      <XCircle size={16} /> {processing[`org_${org.orgId}`] === 'rejecting' ? 'Rejecting...' : 'Reject'}
                    </button>
                    <button 
                      onClick={() => handleApproveOrg(org.orgId)}
                      disabled={processing[`org_${org.orgId}`]}
                      className="btn btn-primary" 
                      style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--success-color)', border: 'none' }}
                    >
                      <CheckCircle size={16} /> {processing[`org_${org.orgId}`] === 'approving' ? 'Approving...' : 'Approve & Provision'}
                    </button>
                  </div>
                </div>
              ))}
              {pendingOrgs.length === 0 && <p style={{ color: 'var(--text-secondary)' }}>No pending organization registrations.</p>}
            </div>
          </div>

          {/* Content Creator Approvals */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>Content Creator Signups</h3>
              <span style={{ background: 'rgba(245, 158, 11, 0.1)', color: 'var(--warning-color)', padding: '2px 8px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 700 }}>
                {pendingCreators.length} Pending
              </span>
            </div>
            
            <div>
              {pendingCreators.map(creator => (
                <div key={creator.userId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', marginBottom: '1rem', background: 'var(--bg-secondary)' }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--text-primary)' }}>{creator.username}</div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.25rem' }}>
                      <span>Email: {creator.email}</span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={14} /> Applied: {new Date(creator.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button 
                      onClick={() => handleRejectCreator(creator.userId)}
                      disabled={processing[`creator_${creator.userId}`]}
                      className="btn btn-secondary" 
                      style={{ color: 'var(--danger-color)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                    >
                      <XCircle size={16} /> {processing[`creator_${creator.userId}`] === 'rejecting' ? 'Rejecting...' : 'Reject'}
                    </button>
                    <button 
                      onClick={() => handleApproveCreator(creator.userId)}
                      disabled={processing[`creator_${creator.userId}`]}
                      className="btn btn-primary" 
                      style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--success-color)', border: 'none' }}
                    >
                      <CheckCircle size={16} /> {processing[`creator_${creator.userId}`] === 'approving' ? 'Approving...' : 'Approve Creator'}
                    </button>
                  </div>
                </div>
              ))}
              {pendingCreators.length === 0 && <p style={{ color: 'var(--text-secondary)' }}>No pending creator registrations.</p>}
            </div>
          </div>

          {/* Course Rollout Approvals */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>Course Version Rollouts</h3>
              <span style={{ background: 'rgba(245, 158, 11, 0.1)', color: 'var(--warning-color)', padding: '2px 8px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 700 }}>
                {pendingCourses.length} Pending
              </span>
            </div>
            
            <div>
              {pendingCourses.map(course => (
                <div key={course.courseId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', marginBottom: '1rem', background: 'rgba(0,0,0,0.01)' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                      <span style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--text-primary)' }}>{course.title}</span>
                      <span style={{ fontFamily: 'monospace', background: 'var(--border-color)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.8rem', color: 'var(--text-primary)' }}>
                        v{course.version}
                      </span>
                    </div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.5rem' }}>
                      <strong>Category:</strong> {course.category || 'General'} &nbsp;|&nbsp; 
                      <strong>Status:</strong> {course.status}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', flexDirection: 'column' }}>
                    <button 
                      onClick={() => handleApproveCourse(course.courseId)}
                      disabled={processing[`course_${course.courseId}`]}
                      className="btn btn-primary" 
                      style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'center', border: 'none' }}
                    >
                      <CheckCircle size={16} /> {processing[`course_${course.courseId}`] === 'approving' ? 'Publishing...' : 'Publish Version'}
                    </button>
                    <button 
                      onClick={() => handleRejectCourse(course.courseId)}
                      disabled={processing[`course_${course.courseId}`]}
                      className="btn btn-secondary" 
                      style={{ color: 'var(--danger-color)', display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'center' }}
                    >
                      <XCircle size={16} /> {processing[`course_${course.courseId}`] === 'rejecting' ? 'Returning...' : 'Return to Draft'}
                    </button>
                  </div>
                </div>
              ))}
              {pendingCourses.length === 0 && <p style={{ color: 'var(--text-secondary)' }}>No pending course rollouts.</p>}
            </div>
          </div>

        </div>
      )}
      
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default ApprovalsTab;

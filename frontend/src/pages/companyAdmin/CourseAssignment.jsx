import React, { useState, useEffect } from 'react';
import { BookOpen, Calendar, Send, Users, RefreshCw } from 'lucide-react';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const CourseAssignment = () => {
  const [courses, setCourses] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState(null);
  
  // Form State
  const [selectedCourse, setSelectedCourse] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('');
  const [deadline, setDeadline] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [coursesRes, employeesRes, enrollmentsRes] = await Promise.all([
        api.get('/company-admin/courses'),
        api.get('/company-admin/employees'),
        api.get('/company-admin/enrollments')
      ]);
      setCourses(coursesRes.data || []);
      setEmployees(employeesRes.data || []);
      setEnrollments(enrollmentsRes.data || []);
    } catch (err) {
      console.error('Failed to load assignments data:', err);
      setToast({ message: 'Failed to retrieve available courses or employees.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Filter out pending approvals for target assignment groups
  const activeEmployees = employees.filter(e => e.status !== 'PENDING');
  
  // Group employees by department
  const departments = [...new Set(activeEmployees.map(e => e.department).filter(Boolean))];

  // Calculate target employee count based on selection
  const getSelectedEmployeeIds = () => {
    if (selectedGroup === 'all') {
      return activeEmployees.map(e => e.userId);
    }
    if (selectedGroup) {
      return activeEmployees
        .filter(e => e.department && e.department.toLowerCase() === selectedGroup.toLowerCase())
        .map(e => e.userId);
    }
    return [];
  };

  const targetCount = getSelectedEmployeeIds().length;

  const handleDispatch = async () => {
    if (!selectedCourse) {
      setToast({ message: 'Please select a course.', type: 'error' });
      return;
    }
    if (!selectedGroup) {
      setToast({ message: 'Please select a target group.', type: 'error' });
      return;
    }
    
    const employeeIds = getSelectedEmployeeIds();
    if (employeeIds.length === 0) {
      setToast({ message: 'No active employees in the selected group.', type: 'error' });
      return;
    }

    setSubmitting(true);
    try {
      // Format deadline as LocalDateTime: "YYYY-MM-DDT00:00:00"
      const formattedDeadline = deadline ? `${deadline}T23:59:59` : null;

      await api.post('/company-admin/enrollments/batch', {
        courseId: parseInt(selectedCourse, 10),
        employeeIds,
        deadline: formattedDeadline
      });

      setToast({ message: `Successfully enrolled ${employeeIds.length} employees!`, type: 'success' });
      // Reset form
      setSelectedCourse('');
      setSelectedGroup('');
      setDeadline('');
      // Refresh enrollments list
      fetchData();
    } catch (err) {
      const errorMsg = 'Failed to dispatch assignments.';
      setToast({ message: errorMsg, type: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  // Group active enrollments by Course to show aggregated course campaign progress
  const activeGroupAssignments = [];
  const courseGroups = {};

  enrollments.forEach(enrollment => {
    const cid = enrollment.courseId;
    if (!courseGroups[cid]) {
      courseGroups[cid] = {
        courseId: cid,
        total: 0,
        completed: 0,
        progressSum: 0,
        deadline: enrollment.deadline
      };
    }
    courseGroups[cid].total += 1;
    if (enrollment.status === 'COMPLETED' || enrollment.progressPercentage === 100) {
      courseGroups[cid].completed += 1;
    }
    courseGroups[cid].progressSum += enrollment.progressPercentage || 0;
  });

  Object.values(courseGroups).forEach(grp => {
    const courseObj = courses.find(c => c.courseId === grp.courseId);
    if (courseObj) {
      activeGroupAssignments.push({
        courseId: grp.courseId,
        title: courseObj.title,
        total: grp.total,
        completed: grp.completed,
        avgProgress: Math.round(grp.progressSum / grp.total),
        deadline: grp.deadline
      });
    }
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Batch Course Assignment
        </h2>
        <button 
          onClick={fetchData}
          className="btn btn-secondary"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>
      
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Assign courses mapped by the Super Admin to groups of employees. Deadlines trigger automated email reminders.
      </p>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading course assignment system...
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
          {/* Assignment Form */}
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1.5rem' }}>New Assignment</h3>
            
            <div className="form-group" style={{ marginBottom: '1.5rem' }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><BookOpen size={16}/> Select Course</label>
              <select 
                className="form-input"
                value={selectedCourse}
                onChange={(e) => setSelectedCourse(e.target.value)}
              >
                <option value="">-- Choose a Course --</option>
                {courses.map(course => (
                  <option key={course.courseId} value={course.courseId}>{course.title}</option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: '1.5rem' }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><Users size={16}/> Target Group</label>
              <select 
                className="form-input" 
                value={selectedGroup} 
                onChange={(e) => setSelectedGroup(e.target.value)}
              >
                <option value="">-- Choose Target Group --</option>
                <option value="all">All Employees ({activeEmployees.length})</option>
                {departments.map((dept, idx) => (
                  <option key={idx} value={dept}>
                    {dept} Department ({activeEmployees.filter(e => e.department === dept).length})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: '2rem' }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}><Calendar size={16}/> Completion Deadline</label>
              <input 
                type="date" 
                className="form-input" 
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
              />
            </div>

            <button 
              onClick={handleDispatch}
              disabled={submitting}
              className="btn btn-primary" 
              style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
            >
              <Send size={18} /> {submitting ? 'Dispatching...' : `Dispatch to ${targetCount} Employees`}
            </button>
          </div>

          {/* Active Assignments */}
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem' }}>Active Group Campaigns</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {activeGroupAssignments.map((campaign, idx) => {
                const isOverdue = campaign.deadline && new Date(campaign.deadline) < new Date();
                
                return (
                  <div key={idx} className="glass-panel" style={{ padding: '1.5rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                      <div>
                        <h4 style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--text-primary)' }}>{campaign.title}</h4>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                          Assigned to: {campaign.total} Employees
                        </div>
                      </div>
                      {campaign.deadline && (
                        <div style={{ 
                          background: isOverdue ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)', 
                          color: isOverdue ? 'var(--danger-color)' : 'var(--success-color)', 
                          padding: '4px 8px', 
                          borderRadius: '4px', 
                          fontSize: '0.75rem', 
                          fontWeight: 700 
                        }}>
                          {isOverdue 
                            ? 'OVERDUE' 
                            : `DUE ${new Date(campaign.deadline).toLocaleDateString()}`}
                        </div>
                      )}
                    </div>
                    <div style={{ width: '100%', height: '6px', background: 'var(--border-color)', borderRadius: '3px', overflow: 'hidden', marginBottom: '0.5rem' }}>
                      <div style={{ width: `${campaign.avgProgress}%`, height: '100%', background: campaign.avgProgress === 100 ? 'var(--success-color)' : 'var(--primary-color)' }} />
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', justifyContent: 'space-between' }}>
                      <span>{campaign.completed} / {campaign.total} Completed</span>
                      <span style={{ fontWeight: 600, color: 'var(--primary-color)' }}>
                        {campaign.avgProgress}% Average Progress
                      </span>
                    </div>
                  </div>
                );
              })}

              {activeGroupAssignments.length === 0 && (
                <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                  No active learning campaigns currently deployed.
                </div>
              )}
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

export default CourseAssignment;

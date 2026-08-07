import React, { useState, useEffect } from 'react';
import { UploadCloud, GripVertical, FileVideo, FileText, CheckCircle, Plus, BookOpen, FolderOpen, AlertTriangle } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const CourseBuilder = () => {
  const { user } = useAuth();
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [lessons, setLessons] = useState([]);
  
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadedMedia, setUploadedMedia] = useState(null);
  const [toast, setToast] = useState(null);
  const [newLessonTitle, setNewLessonTitle] = useState('');
  const [newModuleTitle, setNewModuleTitle] = useState('');
  const [newLessonType, setNewLessonType] = useState('VIDEO');
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);

  // Derived: the full course object for the currently selected course
  const selectedCourse = courses.find(c => String(c.courseId) === String(selectedCourseId)) || null;

  useEffect(() => {
    const fetchCourses = async () => {
      if (!user || !user.userId) return;
      try {
        const response = await api.get(`/creator/courses?creatorId=${user.userId}`);
        setCourses(response.data || []);
        if (response.data && response.data.length > 0) {
          setSelectedCourseId(response.data[0].courseId);
        }
      } catch (err) {
        console.error("Failed to fetch courses", err);
      }
    };
    fetchCourses();
  }, [user]);

  useEffect(() => {
    if (!selectedCourseId) return;
    const fetchLessons = async () => {
      try {
        const response = await api.get(`/creator/courses/${selectedCourseId}/lessons`);
        setLessons(response.data || []);
      } catch (err) {
        console.error("Failed to fetch lessons", err);
      }
    };
    fetchLessons();
  }, [selectedCourseId]);

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    setUploadProgress(0);
    
    // Simulate progress for UI feel
    const interval = setInterval(() => setUploadProgress(p => Math.min(p + 10, 90)), 100);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await api.post('/creator/media/upload', formData);
      clearInterval(interval);
      setUploadProgress(100);
      setUploadedMedia({
        fileName: file.name,
        url: response.data.url
      });
      setToast({ message: 'Media uploaded successfully!', type: 'success' });
      setUploading(false);
    } catch (err) {
      clearInterval(interval);
      console.error("Upload failed", err);
      setToast({ message: 'Failed to upload media.', type: 'error' });
      setUploading(false);
    }
  };

  const handleAddLesson = async () => {
    if (!newLessonTitle || !uploadedMedia || !newModuleTitle) {
      setToast({ message: 'Please provide Module Title, Lesson Title, and upload media.', type: 'error' });
      return;
    }

    try {
      const payload = {
        title: newLessonTitle,
        moduleTitle: newModuleTitle,
        lessonType: newLessonType,
        contentUrl: uploadedMedia.url,
        seqOrder: lessons.length + 1
      };
      
      const response = await api.post(`/creator/courses/${selectedCourseId}/lessons`, payload);
      setLessons([...lessons, response.data]);
      setToast({ message: 'Lesson added successfully!', type: 'success' });
      
      // Reset form (keep module title to make it easy to add more lessons to same module)
      setNewLessonTitle('');
      setUploadedMedia(null);
      setUploadProgress(0);
      setUploading(false);
    } catch (err) {
      console.error("Failed to add lesson", err);
      setToast({ message: 'Failed to add lesson.', type: 'error' });
    }
  };

  const groupedLessons = lessons.reduce((acc, lesson) => {
    const mod = lesson.moduleTitle || 'Uncategorized Module';
    if (!acc[mod]) acc[mod] = [];
    acc[mod].push(lesson);
    return acc;
  }, {});

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Course Builder
        </h2>
        
        {courses.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', background: 'var(--bg-secondary)', padding: '0.5rem 1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
            <BookOpen size={18} color="var(--text-secondary)" />
            <select 
              value={selectedCourseId} 
              onChange={(e) => setSelectedCourseId(e.target.value)}
              style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', fontWeight: 600, outline: 'none', cursor: 'pointer' }}
            >
              {courses.map(c => (
                <option key={c.courseId} value={c.courseId}>
                  {c.title} (v{c.version}) — {c.status === 'PENDING_APPROVAL' ? '⏳ Pending' : c.status === 'PUBLISHED' ? '✅ Published' : '📝 Draft'}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {!selectedCourseId ? (
        <div style={{ padding: '3rem', textAlign: 'center', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-lg)' }}>
          <p style={{ color: 'var(--text-secondary)' }}>You don't have any courses yet. Create one from the Overview tab.</p>
        </div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
            {/* Lesson Assembly */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>Lesson Structure</h3>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                {Object.keys(groupedLessons).map((modTitle, modIndex) => (
                  <div key={modTitle} style={{ background: 'var(--bg-secondary)', borderRadius: 'var(--radius-lg)', padding: '1rem', border: '1px solid var(--border-color)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
                      <FolderOpen size={20} color="var(--primary-color)" />
                      <h4 style={{ fontSize: '1.05rem', fontWeight: 600 }}>Module {modIndex + 1}: {modTitle}</h4>
                    </div>
                    
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                      {groupedLessons[modTitle].map((lesson, lessonIndex) => (
                        <div key={lesson.lessonId} className="glass-panel" style={{ padding: '0.75rem 1rem', display: 'flex', alignItems: 'center', gap: '1rem', cursor: 'grab', background: 'white' }}>
                          <GripVertical size={18} color="var(--text-secondary)" />
                          <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            {lesson.lessonType === 'VIDEO' ? <FileVideo size={18} /> : <FileText size={18} />}
                          </div>
                          <div style={{ flexGrow: 1 }}>
                            <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>Lesson {lessonIndex + 1}: {lesson.title}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
                
                {lessons.length === 0 && (
                  <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', fontStyle: 'italic', padding: '1rem' }}>No modules or lessons added yet.</p>
                )}
              </div>
            </div>

            {/* Media Upload */}
            <div className="glass-panel" style={{ padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1.5rem' }}>Add New Lesson</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
                Upload media using our local Dummy Storage System. In production, this proxies to a Cloud CDN.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '2rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>Module Name <span style={{ color: 'red' }}>*</span></label>
                  <input type="text" className="form-input" required value={newModuleTitle} onChange={(e) => setNewModuleTitle(e.target.value)} placeholder="e.g. Introduction to React" list="module-list" />
                  <datalist id="module-list">
                    {Object.keys(groupedLessons).map(mod => <option key={mod} value={mod} />)}
                  </datalist>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>Lesson Title <span style={{ color: 'red' }}>*</span></label>
                    <input type="text" className="form-input" required value={newLessonTitle} onChange={(e) => setNewLessonTitle(e.target.value)} placeholder="e.g. Setting up the environment" />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>Lesson Type <span style={{ color: 'red' }}>*</span></label>
                    <select className="form-input" required value={newLessonType} onChange={(e) => setNewLessonType(e.target.value)}>
                      <option value="VIDEO">Video</option>
                      <option value="PDF">PDF Document</option>
                    </select>
                  </div>
                </div>
              </div>

              <label style={{ border: '2px dashed var(--border-color)', borderRadius: 'var(--radius-lg)', padding: '2rem 1rem', textAlign: 'center', background: 'var(--bg-secondary)', marginBottom: '1rem', transition: 'all 0.2s', cursor: 'pointer', display: 'block' }}>
                <input type="file" required style={{ display: 'none' }} onChange={handleFileUpload} accept="video/*,application/pdf" />
                <div style={{ width: '50px', height: '50px', borderRadius: '50%', background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1rem auto' }}>
                  <UploadCloud size={24} />
                </div>
                <h4 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.25rem' }}>Click to Upload Media File <span style={{ color: 'red' }}>*</span></h4>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Max file size: 50MB</p>
              </label>

              {uploading && (
                <div style={{ padding: '1rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)', marginBottom: '1rem' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem' }}>Uploading...</h4>
                  <div style={{ width: '100%', height: '4px', background: 'var(--border-color)', borderRadius: '2px' }}>
                    <div style={{ width: `${uploadProgress}%`, height: '100%', background: 'var(--primary-color)', transition: 'width 0.2s' }} />
                  </div>
                </div>
              )}

              {uploadedMedia && !uploading && (
                <div style={{ padding: '1rem', border: '1px solid var(--success-color)', borderRadius: 'var(--radius-md)', background: 'rgba(16, 185, 129, 0.05)', marginBottom: '1.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <CheckCircle size={24} color="var(--success-color)" />
                    <div style={{ flexGrow: 1 }}>
                      <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--success-color)' }}>Upload Complete: {uploadedMedia.fileName}</div>
                    </div>
                  </div>
                </div>
              )}

              <button onClick={handleAddLesson} disabled={!uploadedMedia || !newLessonTitle || !newModuleTitle} className="btn btn-primary" style={{ width: '100%', opacity: (!uploadedMedia || !newLessonTitle || !newModuleTitle) ? 0.5 : 1 }}>
                Add Lesson to Course
              </button>
            </div>
          </div>

          <div style={{ marginTop: '2rem' }}>
            {selectedCourse?.status === 'DRAFT' && showSubmitConfirm && (
              <div style={{ background: 'rgba(245, 158, 11, 0.1)', border: '1px solid var(--warning-color)', borderRadius: 'var(--radius-md)', padding: '1rem', marginBottom: '1.5rem', display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <AlertTriangle size={24} color="var(--warning-color)" style={{ flexShrink: 0 }} />
                <div>
                  <h4 style={{ fontWeight: 600, color: '#b45309', marginBottom: '0.25rem' }}>Ready to Submit?</h4>
                  <p style={{ fontSize: '0.9rem', color: '#b45309', opacity: 0.9, marginBottom: '1rem' }}>
                    Once you submit this course for approval, you will not be able to make changes to this version again. 
                    If you wish to make changes later, a new version will be created. Save as draft if you are still working on it.
                  </p>
                  <div style={{ display: 'flex', gap: '1rem' }}>
                    <button className="btn" style={{ background: 'white', color: 'var(--text-primary)', border: '1px solid var(--border-color)' }} onClick={() => setShowSubmitConfirm(false)}>
                      Cancel
                    </button>
                    <button className="btn btn-primary" style={{ background: 'var(--warning-color)' }} onClick={async () => {
                      if (!selectedCourseId) return;
                      try {
                        const response = await api.post(`/creator/courses/${selectedCourseId}/submit`);
                        setToast({ message: 'Course submitted for approval successfully!', type: 'success' });
                        // Update the local course list to reflect the new status
                        setCourses(courses.map(c => String(c.courseId) === String(selectedCourseId) ? { ...c, status: 'PENDING_APPROVAL' } : c));
                        setShowSubmitConfirm(false);
                      } catch (err) {
                        console.error('Failed to submit course', err);
                        setToast({ message: 'Failed to submit course. It might already be pending approval.', type: 'error' });
                      }
                    }}>
                      Confirm Submission
                    </button>
                  </div>
                </div>
              </div>
            )}

            {!showSubmitConfirm && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                <button className="btn btn-secondary" disabled={selectedCourse?.status !== 'DRAFT'}>
                  Save Draft
                </button>
                <button 
                  className="btn btn-primary" 
                  disabled={selectedCourse?.status !== 'DRAFT'}
                  onClick={() => setShowSubmitConfirm(true)}>
                  {selectedCourse?.status === 'PENDING_APPROVAL' ? 'Submitted for Approval' : selectedCourse?.status === 'PUBLISHED' ? 'Published' : 'Submit for Approval'}
                </button>
              </div>
            )}
          </div>
        </>
      )}
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default CourseBuilder;

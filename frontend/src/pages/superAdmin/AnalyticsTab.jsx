import React, { useState, useEffect } from 'react';
import TelemetryVisualizer from '../../components/analytics/TelemetryVisualizer';
import { Share2, BarChart2, Loader2, BrainCircuit } from 'lucide-react';
import api from '../../services/api';
import aiApi from '../../services/aiApi';
import Toast from '../../components/shared/Toast';

const AnalyticsTab = () => {
  const [courses, setCourses] = useState([]);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [performance, setPerformance] = useState(null);
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [fetchingMetrics, setFetchingMetrics] = useState(false);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        const res = await api.get('/super-admin/courses');
        setCourses(res.data);
        if (res.data.length > 0) {
          setSelectedCourse(res.data[0].courseId);
        }
      } catch (err) {
        console.error('Failed to load courses', err);
        // Fallback for when the new endpoint is not yet loaded after restart
        setCourses([{ courseId: '1', title: 'Loading backend courses...' }]);
      } finally {
        setLoading(false);
      }
    };
    fetchCourses();
  }, []);

  useEffect(() => {
    if (!selectedCourse) return;

    const fetchMetrics = async () => {
      setFetchingMetrics(true);
      setPerformance(null);
      setInsights(null);
      try {
        const perfRes = await api.get(`/super-admin/courses/${selectedCourse}/performance`);
        setPerformance(perfRes.data);

        // Fetch AI-driven insights for the course
        try {
          const insightRes = await aiApi.getPlatformInsights(selectedCourse);
          setInsights(insightRes.data);
        } catch (e) {
          console.warn('No insights found for this course/lesson yet.');
        }
      } catch (err) {
        console.error('Failed to fetch course metrics', err);
      } finally {
        setFetchingMetrics(false);
      }
    };

    fetchMetrics();
  }, [selectedCourse]);

  const handleShare = async () => {
    try {
      await api.post(`/super-admin/courses/${selectedCourse}/video-insights/share`);
      setToast({ message: 'Insight report shared with the Content Creator successfully!', type: 'success' });
    } catch (err) {
      setToast({ message: 'Failed to share insight report.', type: 'error' });
    }
  };

  return (
    <div>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        Global Content Analytics
      </h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        View aggregated, anonymized telemetry for courses across all organizations. Identify struggle points and share insights directly with Content Creators.
      </p>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading content library...
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem' }}>
          {/* Course Selector & High Level Metrics */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem' }}>Select Course</h3>
              <select 
                className="form-input" 
                value={selectedCourse} 
                onChange={(e) => setSelectedCourse(e.target.value)}
              >
                {courses.map(c => (
                  <option key={c.courseId} value={c.courseId}>{c.title}</option>
                ))}
              </select>
            </div>

            {fetchingMetrics ? (
              <div className="glass-panel flex justify-center items-center py-10 text-indigo-600">
                <Loader2 className="w-8 h-8 animate-spin" />
              </div>
            ) : performance ? (
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <BarChart2 size={18} color="var(--primary-color)" /> ROI Summary
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600 }}>Avg Completion Rate</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>{(performance.completionRate || 0).toFixed(1)}%</div>
                  </div>
                  <div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600 }}>Avg Quiz Score</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--success-color)' }}>{performance.averageQuizScore.toFixed(1)}</div>
                  </div>
                  <div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600 }}>Avg Time-to-Competency</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>{performance.averageTimeToCompetencyDays.toFixed(1)} days</div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="glass-panel" style={{ padding: '1.5rem', color: 'var(--text-secondary)' }}>
                No performance data available for this course yet.
              </div>
            )}
          </div>

          {/* Telemetry Visualizer & Sharing */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {!fetchingMetrics && Array.isArray(insights) && insights.length > 0 ? (
              <div className="glass-panel" style={{ padding: '1.5rem', background: '#f8fafc', border: '1px solid #e2e8f0' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <BrainCircuit size={18} color="var(--primary-color)" /> Platform AI Diagnosis
                </h3>
                <div style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '1.5rem' }}>
                  {insights.map((insight, idx) => (
                    <div key={idx} style={{ marginBottom: '1rem', padding: '1rem', background: 'rgba(37,99,235,0.04)', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                      <p style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>Lesson {insight.lessonId}</p>
                      <p style={{ marginBottom: '0.25rem' }}>{insight.insightSummary}</p>
                      {insight.creatorSuggestion && <p style={{ color: 'var(--primary-color)', fontWeight: 500 }}>Action: {insight.creatorSuggestion}</p>}
                      {insight.sessionsAnalyzed && <p style={{ fontSize: '0.8rem', fontStyle: 'italic', marginTop: '0.25rem' }}>Based on {insight.sessionsAnalyzed} platform-wide sessions</p>}
                    </div>
                  ))}
                </div>
                <button onClick={handleShare} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Share2 size={16} /> Share Insight Report with Creator
                </button>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.75rem', fontStyle: 'italic' }}>
                  Sharing this report preserves learner anonymity. Only the aggregated AI diagnosis will be visible to the author.
                </p>
              </div>
            ) : !fetchingMetrics && (
              <div className="glass-panel flex flex-col items-center justify-center p-12 text-center text-slate-500">
                <BarChart2 className="w-12 h-12 mb-4 text-slate-300" />
                <p>Not enough telemetry data recorded yet to generate AI insights and hotspots.</p>
              </div>
            )}
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

export default AnalyticsTab;

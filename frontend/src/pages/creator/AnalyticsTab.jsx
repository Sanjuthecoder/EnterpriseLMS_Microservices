import React, { useState, useEffect } from 'react';
import { Share2, Lightbulb, Video, AlertTriangle, RefreshCw } from 'lucide-react';
import api from '../../services/api';

const AnalyticsTab = () => {
  const [insights, setInsights] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchInsights = async () => {
    setLoading(true);
    try {
      const response = await api.get('/creator/video-insights');
      setInsights(response.data || []);
    } catch (err) {
      console.error('Failed to fetch video insights', err);
      setInsights([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInsights();
  }, []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.25rem', color: 'var(--text-primary)' }}>
            Instructional Insights Inbox
          </h2>
          <p style={{ color: 'var(--text-secondary)' }}>
            Review anonymized video telemetry insights shared by the Super Admin. Use these data points to optimize your next course version.
          </p>
        </div>
        <button onClick={fetchInsights} className="btn btn-secondary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading insights...</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
          {insights.map((insight, idx) => (
            <div key={insight.lessonId || idx} className="glass-panel" style={{ padding: '2rem', display: 'flex', gap: '2rem', alignItems: 'flex-start' }}>
              
              <div style={{ width: '60px', height: '60px', borderRadius: '50%', background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <Lightbulb size={30} />
              </div>

              <div style={{ flexGrow: 1 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {insight.courseName}
                  </h3>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Share2 size={14} /> Shared by Super Admin
                  </span>
                </div>
                
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                  <Video size={16} /> {insight.lessonTitle}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem', background: 'var(--bg-secondary)', padding: '1.5rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
                  <div>
                    <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.75rem' }}>Detected Hotspots</h4>
                    {(insight.feedbackHotspots || []).map((hotspot, i) => (
                      <div key={i} style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--danger-color)', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                        <AlertTriangle size={14} /> {hotspot}
                      </div>
                    ))}
                  </div>
                  <div>
                    <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>AI Suggested Action</h4>
                    <p style={{ fontSize: '0.95rem', color: 'var(--text-primary)', lineHeight: 1.6 }}>
                      {insight.suggestedAction}
                    </p>
                  </div>
                </div>
                
                <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end' }}>
                  <button className="btn btn-primary">Edit Course (Create Version 2)</button>
                </div>
              </div>

            </div>
          ))}

          {insights.length === 0 && (
            <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              <Lightbulb size={40} style={{ marginBottom: '1rem', opacity: 0.4 }} />
              <p>No insights shared yet.</p>
              <p style={{ fontSize: '0.9rem', marginTop: '0.5rem' }}>Super Admins will share telemetry reports here once your courses gain sufficient learner engagement.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AnalyticsTab;

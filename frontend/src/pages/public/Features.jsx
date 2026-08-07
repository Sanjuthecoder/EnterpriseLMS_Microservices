import React from 'react';
import { Target, Cpu, Activity, Layout } from 'lucide-react';

const Features = () => {
  return (
    <div style={{ padding: '4rem 2rem', maxWidth: '1200px', margin: '0 auto', minHeight: '80vh' }}>
      <div style={{ textAlign: 'center', marginBottom: '4rem' }}>
        <h1 style={{ fontSize: '3rem', fontWeight: 800, marginBottom: '1rem', color: '#0f172a' }}>Features & ROI</h1>
        <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', maxWidth: '700px', margin: '0 auto' }}>
          Discover how our deeply integrated telemetry algorithm saves thousands of corporate hours by tailoring the learning journey to each employee.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4rem', alignItems: 'center', marginBottom: '6rem' }}>
        <div>
          <h2 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '1rem' }}>Pre-Quiz Adaptive Gating</h2>
          <p style={{ fontSize: '1.05rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '1.5rem' }}>
            Before beginning any course, employees undergo a low-stress diagnostic. The system identifies exactly which concepts they already know and marks those lessons as <strong>OPTIONAL</strong>.
          </p>
          <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <li style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
              <Target color="var(--primary-color)" />
              <span><strong>Eliminate Redundancy:</strong> Don't pay employees to re-learn what they know.</span>
            </li>
            <li style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
              <Cpu color="var(--primary-color)" />
              <span><strong>AI Curated Paths:</strong> Dynamically generate the shortest path to competency.</span>
            </li>
          </ul>
        </div>
        <div className="glass-panel" style={{ background: '#f1f5f9', minHeight: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-lg)' }}>
          <div style={{ padding: '2rem', background: 'white', borderRadius: '12px', boxShadow: 'var(--shadow-md)', width: '80%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem', borderBottom: '1px solid #e2e8f0' }}>
              <span style={{ fontWeight: 600 }}>Virtual DOM Deep Dive</span>
              <span style={{ background: 'var(--warning-color)', color: 'white', padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600 }}>RECOMMENDED</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem' }}>
              <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>Context API Basics</span>
              <span style={{ background: '#e2e8f0', color: '#64748b', padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600 }}>OPTIONAL</span>
            </div>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4rem', alignItems: 'center' }}>
        <div className="glass-panel" style={{ background: '#0f172a', minHeight: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-lg)', padding: '2rem' }}>
           <img src="/features_telemetry.png" alt="Analytics Graph" style={{ width: '100%', borderRadius: '8px' }} />
        </div>
        <div>
          <h2 style={{ fontSize: '2rem', fontWeight: 700, marginBottom: '1rem' }}>xAPI Post-Quiz Uplift</h2>
          <p style={{ fontSize: '1.05rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '1.5rem' }}>
            A training platform without ROI is just an expense. We calculate the exact difference between the Pre-Quiz and Post-Quiz.
          </p>
          <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <li style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
              <Activity color="var(--success-color)" />
              <span><strong>Measure True Uplift:</strong> Prove that training budgets resulted in net-new skills acquired.</span>
            </li>
            <li style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
              <Layout color="var(--success-color)" />
              <span><strong>Creator Feedback:</strong> Super Admins can share telemetry hotspots with content creators to rapidly improve the next course version.</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default Features;

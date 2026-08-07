import React from 'react';

const About = () => {
  return (
    <div style={{ padding: '4rem 2rem', maxWidth: '800px', margin: '0 auto', minHeight: '80vh', textAlign: 'center' }}>
      <h1 style={{ fontSize: '3rem', fontWeight: 800, marginBottom: '1.5rem', color: '#0f172a' }}>About Us</h1>
      <p style={{ fontSize: '1.2rem', color: 'var(--text-secondary)', lineHeight: 1.8, marginBottom: '2rem' }}>
        We built EnterpriseLMS because traditional corporate training is broken. Organizations spend billions on one-size-fits-all training modules that bore experts and overwhelm beginners. 
      </p>
      <div className="glass-panel" style={{ textAlign: 'left', padding: '3rem', background: 'white' }}>
        <h3 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1rem' }}>Our Mission</h3>
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7, marginBottom: '2rem' }}>
          Our mission is to end redundant training hours by bringing intelligent, adaptive pathways to enterprise companies. We believe that by utilizing advanced xAPI telemetry—measuring not just scores, but cognitive load and hesitations—we can build a learning experience that respects the employee's time and guarantees measurable ROI for the employer.
        </p>
        <h3 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1rem' }}>The Future of Work</h3>
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>
          With our dynamic course versioning and continuous feedback loops, we ensure your organization's knowledge base stays at the cutting edge of industry standards.
        </p>
      </div>
    </div>
  );
};

export default About;

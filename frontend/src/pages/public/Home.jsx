import React from 'react';
import { Link } from 'react-router-dom';
import { TrendingUp, ShieldCheck, Clock, Zap, BookOpen, BarChart } from 'lucide-react';

const Home = () => {
  return (
    <div style={{ fontFamily: 'var(--font-family)', color: 'var(--text-primary)' }}>
      {/* Hero Section */}
      <section style={{
        minHeight: '85vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '4rem 2rem',
        background: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{
          maxWidth: '1200px',
          width: '100%',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '4rem',
          alignItems: 'center',
          position: 'relative',
          zIndex: 10
        }}>
          <div>
            <div style={{
              display: 'inline-block',
              padding: '0.5rem 1rem',
              background: 'rgba(37, 99, 235, 0.1)',
              color: 'var(--primary-color)',
              borderRadius: '2rem',
              fontWeight: 600,
              fontSize: '0.85rem',
              marginBottom: '1.5rem',
              letterSpacing: '0.5px'
            }}>
              Solving the Employee Training Problem
            </div>
            <h1 style={{ fontSize: '3.5rem', fontWeight: 800, lineHeight: 1.1, marginBottom: '1.5rem', color: '#0f172a' }}>
              Corporate Training,<br />
              <span style={{ color: 'var(--primary-color)' }}>Adaptively Intelligent.</span>
            </h1>
            <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '2.5rem', maxWidth: '500px' }}>
              We reduce your training costs and save valuable time by utilizing strict xAPI monitoring to deliver industry up-to-date materials exactly where skill gaps exist.
            </p>
            <div style={{ display: 'flex', gap: '1rem' }}>
              <Link to="/auth" className="btn btn-primary" style={{ padding: '1rem 2rem', fontSize: '1.05rem', boxShadow: '0 10px 25px rgba(37,99,235,0.3)' }}>
                Get Started
              </Link>
              <Link to="/features" className="btn btn-secondary" style={{ padding: '1rem 2rem', fontSize: '1.05rem' }}>
                View ROI Features
              </Link>
            </div>
          </div>
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <img
              src="/hero_adaptive.png"
              alt="Adaptive Learning Illustration"
              style={{
                width: '100%',
                maxWidth: '600px',
                borderRadius: 'var(--radius-xl)',
                boxShadow: 'var(--shadow-lg)',
                transform: 'perspective(1000px) rotateY(-5deg) rotateX(5deg)',
                transition: 'transform 0.5s ease'
              }}
            />
          </div>
        </div>
      </section>

      {/* Value Propositions */}
      <section style={{ padding: '6rem 2rem', background: 'var(--bg-secondary)' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '4rem' }}>
            <h2 style={{ fontSize: '2.5rem', fontWeight: 700, marginBottom: '1rem' }}>Why Choose EnterpriseLMS?</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', maxWidth: '600px', margin: '0 auto' }}>
              Built for organizations that demand deep insights, strict monitoring, and measurable skill uplift.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '2rem' }}>
            {/* Value Prop 1 */}
            <div className="glass-panel" style={{ padding: '2.5rem', transition: 'transform 0.3s', cursor: 'default' }} onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-10px)'} onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}>
              <div style={{ background: 'rgba(16, 185, 129, 0.1)', width: '60px', height: '60px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem', color: '#10b981' }}>
                <TrendingUp size={30} />
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Saving Training Costs & Time</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                Our adaptive algorithms identify exact skill gaps via diagnostic pre-quizzes, skipping redundant lessons so employees return to work faster, saving thousands in unneeded training hours.
              </p>
            </div>

            {/* Value Prop 2 */}
            <div className="glass-panel" style={{ padding: '2.5rem', transition: 'transform 0.3s', cursor: 'default' }} onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-10px)'} onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}>
              <div style={{ background: 'rgba(37, 99, 235, 0.1)', width: '60px', height: '60px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem', color: '#2563eb' }}>
                <ShieldCheck size={30} />
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Deep & Strict Monitoring</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                We don't just track pass/fail. We monitor video rewinds, pauses, and cognitive load (answer hesitations) via xAPI telemetry to guarantee true concept mastery.
              </p>
            </div>

            {/* Value Prop 3 */}
            <div className="glass-panel" style={{ padding: '2.5rem', transition: 'transform 0.3s', cursor: 'default' }} onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-10px)'} onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}>
              <div style={{ background: 'rgba(245, 158, 11, 0.1)', width: '60px', height: '60px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem', color: '#f59e0b' }}>
                <BookOpen size={30} />
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Industry Up-to-Date Materials</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                Courses are dynamically updated based on employee feedback and strict course analytics. Our immutable versioning ensures learners always receive cutting-edge knowledge without disruption.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Showcase */}
      <section style={{ padding: '6rem 2rem', background: '#0f172a', color: 'white' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4rem', alignItems: 'center' }}>
          <div>
            <img
              src="/features_telemetry.png"
              alt="Deep Analytics Telemetry"
              style={{
                width: '100%',
                borderRadius: 'var(--radius-xl)',
                boxShadow: '0 20px 40px rgba(0,0,0,0.5)',
                border: '1px solid #334155'
              }}
            />
          </div>
          <div>
            <h2 style={{ fontSize: '2.5rem', fontWeight: 700, marginBottom: '1.5rem', color: 'white' }}>Data-Driven Learning Intelligence</h2>
            <p style={{ fontSize: '1.1rem', color: '#94a3b8', lineHeight: 1.6, marginBottom: '2rem' }}>
              Say goodbye to blind completion rates. Our proprietary analytics engine delivers the deepest insights into how your employees learn.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <div style={{ background: '#1e293b', padding: '10px', borderRadius: '8px', color: '#3b82f6' }}><Zap size={24} /></div>
                <div>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '0.25rem' }}>Real-time Course Adjustments</h4>
                  <p style={{ color: '#94a3b8', fontSize: '0.9rem', lineHeight: 1.5 }}>Our AI detects "boredom hotspots" and "cognitive overload", allowing creators to optimize videos actively.</p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <div style={{ background: '#1e293b', padding: '10px', borderRadius: '8px', color: '#10b981' }}><BarChart size={24} /></div>
                <div>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '0.25rem' }}>Pre/Post Skill Uplift Reports</h4>
                  <p style={{ color: '#94a3b8', fontSize: '0.9rem', lineHeight: 1.5 }}>Prove the exact ROI of your training. We measure the exact delta in competency before and after the course.</p>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <div style={{ background: '#1e293b', padding: '10px', borderRadius: '8px', color: '#f59e0b' }}><Clock size={24} /></div>
                <div>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '0.25rem' }}>Regular Employee Updates</h4>
                  <p style={{ color: '#94a3b8', fontSize: '0.9rem', lineHeight: 1.5 }}>Automated, timed nudges and feedback loops keep employees engaged without micromanagement.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section style={{ padding: '5rem 2rem', textAlign: 'center', background: 'var(--primary-color)', color: 'white' }}>
        <h2 style={{ fontSize: '2.5rem', fontWeight: 700, marginBottom: '1rem' }}>Ready to Modernize Your Training?</h2>
        <p style={{ fontSize: '1.1rem', opacity: 0.9, marginBottom: '2.5rem', maxWidth: '600px', margin: '0 auto 2.5rem auto' }}>
          Join leading organizations saving time and money through adaptive learning and deep xAPI telemetry.
        </p>
        <Link to="/auth" className="btn" style={{ background: 'white', color: 'var(--primary-color)', padding: '1rem 2.5rem', fontSize: '1.1rem', fontWeight: 600, boxShadow: '0 4px 15px rgba(0,0,0,0.1)' }}>
          Launch Your Demo
        </Link>
      </section>
    </div>
  );
};

export default Home;

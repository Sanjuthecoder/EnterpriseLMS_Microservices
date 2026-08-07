import React from 'react';
import { Mail, Phone, MapPin } from 'lucide-react';

const Contact = () => {
  return (
    <div style={{ padding: '4rem 2rem', maxWidth: '1000px', margin: '0 auto', minHeight: '80vh' }}>
      <div style={{ textAlign: 'center', marginBottom: '4rem' }}>
        <h1 style={{ fontSize: '3rem', fontWeight: 800, marginBottom: '1rem', color: '#0f172a' }}>Contact Sales</h1>
        <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)' }}>
          Ready to transform your organization's learning culture? Let's talk.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '4rem' }}>
        <div>
          <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1.5rem' }}>Get in Touch</h3>
            <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <li style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                <Mail color="var(--primary-color)" />
                <span style={{ color: 'var(--text-secondary)' }}>enterprise@lms.com</span>
              </li>
              <li style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                <Phone color="var(--primary-color)" />
                <span style={{ color: 'var(--text-secondary)' }}>+1 (800) 555-0199</span>
              </li>
              <li style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                <MapPin color="var(--primary-color)" />
                <span style={{ color: 'var(--text-secondary)' }}>100 Innovation Drive<br/>San Francisco, CA 94103</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '3rem', background: 'white' }}>
          <form onSubmit={(e) => e.preventDefault()}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
              <div className="form-group">
                <label className="form-label">First Name</label>
                <input type="text" className="form-input" placeholder="Jane" />
              </div>
              <div className="form-group">
                <label className="form-label">Last Name</label>
                <input type="text" className="form-input" placeholder="Doe" />
              </div>
            </div>
            
            <div className="form-group" style={{ marginBottom: '1.5rem' }}>
              <label className="form-label">Work Email</label>
              <input type="email" className="form-input" placeholder="jane@company.com" />
            </div>

            <div className="form-group" style={{ marginBottom: '1.5rem' }}>
              <label className="form-label">Company Size</label>
              <select className="form-input">
                <option>1-50 employees</option>
                <option>51-200 employees</option>
                <option>201-1000 employees</option>
                <option>1000+ employees</option>
              </select>
            </div>

            <div className="form-group" style={{ marginBottom: '2rem' }}>
              <label className="form-label">How can we help?</label>
              <textarea className="form-input" rows="4" placeholder="Tell us about your training challenges..."></textarea>
            </div>

            <button className="btn btn-primary" style={{ width: '100%', padding: '1rem', fontSize: '1.1rem' }}>
              Request a Demo
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Contact;

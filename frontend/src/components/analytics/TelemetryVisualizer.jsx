import React from 'react';

const TelemetryVisualizer = ({ data, title }) => {
  if (!data || data.length === 0) return null;

  // Simple heatmap or bar chart visualization for telemetry data
  // data format expected: [{ label: 'Lesson 1', value: 85 }, ...]
  
  const maxValue = Math.max(...data.map(d => d.value), 100);

  return (
    <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '1.5rem' }}>
      <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: 'var(--text-primary)' }}>{title}</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {data.map((item, index) => {
          const widthPercent = (item.value / maxValue) * 100;
          let barColor = 'var(--primary-color)';
          
          if (item.value < 40) barColor = 'var(--danger-color)';
          else if (item.value < 70) barColor = 'var(--warning-color)';
          else if (item.value >= 85) barColor = 'var(--success-color)';

          return (
            <div key={index} style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div style={{ width: '120px', fontSize: '0.85rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {item.label}
              </div>
              <div style={{ flexGrow: 1, height: '12px', background: 'var(--border-color)', borderRadius: '6px', overflow: 'hidden' }}>
                <div style={{ 
                  width: `${widthPercent}%`, 
                  height: '100%', 
                  background: barColor,
                  borderRadius: '6px',
                  transition: 'width 0.5s ease-out'
                }} />
              </div>
              <div style={{ width: '40px', textAlign: 'right', fontSize: '0.85rem', fontWeight: 600 }}>
                {item.value}%
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default TelemetryVisualizer;

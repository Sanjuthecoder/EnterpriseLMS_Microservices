import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import UpliftResults from '../UpliftResults';
import api from '../../../services/api';

jest.mock('../../../services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn()
  }
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({ courseId: '100' })
}));

jest.mock('../../../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { username: 'testuser' }
  })
}));

jest.mock('../../../contexts/ThemeContext', () => ({
  useTheme: () => ({
    themeConfig: { primaryColor: '#2563eb' }
  }),
  ThemeProvider: ({ children }) => <div>{children}</div>
}));

describe('UpliftResults Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders loading state initially', () => {
    api.get.mockImplementation(() => new Promise(() => {})); // Never resolves
    render(<BrowserRouter><UpliftResults /></BrowserRouter>);
    expect(screen.getByText(/Loading results/i)).toBeInTheDocument();
  });

  it('renders certificate request button if ELIGIBLE and handles request', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/uplift')) {
        return Promise.resolve({
          data: {
            courseTitle: 'Test Course',
            preQuizScore: 40.0,
            postQuizScore: 100.0,
            upliftPercent: 60.0,
            upliftReport: {
              conceptsGained: ['Variables'],
              stillStruggling: []
            }
          }
        });
      }
      if (url.includes('/dashboard')) {
        return Promise.resolve({
          data: [{ courseId: 100, certificateStatus: 'ELIGIBLE' }]
        });
      }
      return Promise.reject(new Error('not found'));
    });

    api.post.mockResolvedValue({});

    render(<BrowserRouter><UpliftResults /></BrowserRouter>);

    await waitFor(() => {
      expect(screen.getByText(/Test Course/i)).toBeInTheDocument();
    });

    const requestBtn = screen.getByText(/Request Certificate/i);
    expect(requestBtn).toBeInTheDocument();

    fireEvent.click(requestBtn);

    expect(api.post).toHaveBeenCalledWith('/employees/courses/100/certificate/request');

    await waitFor(() => {
      expect(screen.getByText(/Request Pending Approval/i)).toBeInTheDocument();
    });
  });

  it('renders download button if APPROVED', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/uplift')) {
        return Promise.resolve({ data: { preQuizScore: 50, postQuizScore: 100, upliftPercent: 50 } });
      }
      if (url.includes('/dashboard')) {
        return Promise.resolve({ data: [{ courseId: 100, certificateStatus: 'APPROVED' }] });
      }
      return Promise.reject(new Error('not found'));
    });

    render(<BrowserRouter><UpliftResults /></BrowserRouter>);

    await waitFor(() => {
      expect(screen.getByText(/Download Certificate/i)).toBeInTheDocument();
    });
  });
});
